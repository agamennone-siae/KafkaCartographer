package com.kafkacartographer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KafkaScanner {

    public static class TopicOccurrence {
        public String topic;
        public String type; // Definition or Usage
        public String file;
        public int line;
        public String status; 
        public String project;

        public TopicOccurrence(String topic, String type, String file, int line, String status, String project) {
            this.topic = topic;
            this.type = type;
            this.file = file;
            this.line = line;
            this.status = status;
            this.project = project;
        }
    }

    public static class TopicLink {
        public String topic;
        public String definitionFile;
        public int definitionLine;
        public String definitionProject;
        public String usageFile;
        public int usageLine;
        public String usageProject;

        public TopicLink(String topic, String defFile, int defLine, String defProj, String useFile, int useLine, String useProj) {
            this.topic = topic;
            this.definitionFile = defFile;
            this.definitionLine = defLine;
            this.definitionProject = defProj;
            this.usageFile = useFile;
            this.usageLine = useLine;
            this.usageProject = useProj;
        }
    }

    private final Map<String, TopicOccurrence> symbolDefinitions = new HashMap<>();
    private final List<TopicLink> foundLinks = new ArrayList<>();
    private final List<TopicOccurrence> literals = new ArrayList<>();

    public Map<String, Object> scan(File rootDir) {
        Collection<File> files = FileUtils.listFiles(rootDir, new String[]{"java"}, true);
        
        // Pass 1: Build Symbol Table
        System.out.println("Pass 1: Building symbol table...");
        for (File file : files) {
            try {
                processDefinitions(file);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Pass 2: Find Usages & Resolve
        System.out.println("Pass 2: Finding usages...");
        for (File file : files) {
            try {
                processUsages(file);
            } catch (Exception e) {
                // Ignore
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("links", foundLinks);
        result.put("literals", literals);
        return result;
    }

    private void processDefinitions(File file) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(file);
        String className = file.getName().replace(".java", "");
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(FieldDeclaration n, Void arg) {
                super.visit(n, arg);
                // Listen for static constants
                for (VariableDeclarator var : n.getVariables()) {
                    if (var.getInitializer().isPresent() && var.getInitializer().get().isStringLiteralExpr()) {
                        String value = var.getInitializer().get().asStringLiteralExpr().getValue();
                        String fieldName = var.getNameAsString();
                        String project = extractProjectName(file.getAbsolutePath());
                        
                        TopicOccurrence def = new TopicOccurrence(value, "Definition", file.getAbsolutePath(), n.getRange().get().begin.line, "Defined", project);
                        
                        symbolDefinitions.put(className + "." + fieldName, def);
                        symbolDefinitions.put(fieldName, def);
                    }
                }
            }
        }, null);
    }

    private void processUsages(File file) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(file);
        cu.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(NormalAnnotationExpr n, Void arg) {
                super.visit(n, arg);
                if (n.getNameAsString().equals("KafkaListener")) {
                    n.getPairs().forEach(pair -> {
                        if (pair.getNameAsString().equals("topics") || pair.getNameAsString().equals("value")) {
                            handleTopicUsage(pair.getValue(), file, n.getRange().get().begin.line);
                        }
                    });
                }
            }

            @Override
            public void visit(SingleMemberAnnotationExpr n, Void arg) {
                super.visit(n, arg);
                if (n.getNameAsString().equals("KafkaListener")) {
                    handleTopicUsage(n.getMemberValue(), file, n.getRange().get().begin.line);
                }
            }

            @Override
            public void visit(MethodCallExpr n, Void arg) {
                super.visit(n, arg);
                if (n.getNameAsString().equals("send")) {
                    // Heuristic: check if this file even uses Kafka
                    boolean likelyKafka = cu.getImports().stream()
                        .anyMatch(i -> i.getNameAsString().contains("kafka"));
                    
                    if (!likelyKafka) {
                        // If no kafka import, check the scope (object name)
                        if (n.getScope().isPresent()) {
                            String scope = n.getScope().get().toString().toLowerCase();
                            if (!scope.contains("kafka") && !scope.contains("producer") && !scope.contains("template")) {
                                return; // Probably not Kafka
                            }
                        } else {
                            return; // static or local send in non-kafka file
                        }
                    }

                    if (n.getArguments().size() > 0) {
                        Expression firstArg = n.getArgument(0);
                        
                        // Handle send(new ProducerRecord<>(topic, ...))
                        if (firstArg.isObjectCreationExpr()) {
                            ObjectCreationExpr oce = firstArg.asObjectCreationExpr();
                            if (oce.getType().getNameAsString().equals("ProducerRecord") && oce.getArguments().size() > 0) {
                                handleTopicUsage(oce.getArgument(0), file, n.getRange().get().begin.line);
                            }
                        } else {
                            handleTopicUsage(firstArg, file, n.getRange().get().begin.line);
                        }
                    }
                }
            }
        }, null);
    }

    private void handleTopicUsage(Expression expr, File file, int line) {
        String project = extractProjectName(file.getAbsolutePath());
        if (expr.isStringLiteralExpr()) {
            String topic = expr.asStringLiteralExpr().getValue();
            literals.add(new TopicOccurrence(topic, "Usage", file.getAbsolutePath(), line, "Literal", project));
        } else if (expr.isArrayInitializerExpr()) {
            ArrayInitializerExpr aie = expr.asArrayInitializerExpr();
            for (Expression val : aie.getValues()) {
                handleTopicUsage(val, file, line);
            }
        } else {
            // It's a reference or complex expression
            String raw = expr.toString().replace(".class", "").trim();
            
            // Try to resolve
            TopicOccurrence def = resolveSymbol(raw);
            if (def != null) {
                foundLinks.add(new TopicLink(def.topic, def.file, def.line, def.project, file.getAbsolutePath(), line, project));
            } else {
                // If it looks like a noise variable (single word, lowercase), skip or mark as noise
                if (raw.matches("^[a-z].*") && !raw.contains(".")) {
                   // Possible local variable, skip to avoid "topic" noise
                   return;
                }
                literals.add(new TopicOccurrence(raw, "Usage", file.getAbsolutePath(), line, "UnknownReference", project));
            }
        }
    }

    private String extractProjectName(String path) {
        // Heuristic: finding the folder before 'src' (standard Maven layout)
        // or just looking for known structures.
        // Data format: /app/scan/scan/coreapplications/NetconfPMeas/src/...
        // We want 'NetconfPMeas'.
        
        path = path.replace("\\", "/");
        if (path.contains("/src/")) {
            String temp = path.substring(0, path.indexOf("/src/"));
            return temp.substring(temp.lastIndexOf("/") + 1);
        }
        
        // Fallback for simple folders
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null) {
             // Go up until we find a pom.xml or a recognizable root?
             // Simple fallback: Parent dir name
             return parent.getName();
        }
        return "UnknownProject";
    }

    private TopicOccurrence resolveSymbol(String symbol) {
        // Direct match
        if (symbolDefinitions.containsKey(symbol)) {
            return symbolDefinitions.get(symbol);
        }
        
        // Match by suffix (e.g. MyClass.TOPIC matches TOPIC if MyClass is unique)
        if (symbol.contains(".")) {
            String suffix = symbol.substring(symbol.lastIndexOf(".") + 1);
            if (symbolDefinitions.containsKey(suffix)) {
                return symbolDefinitions.get(suffix);
            }
        }
        
        return null;
    }
}
