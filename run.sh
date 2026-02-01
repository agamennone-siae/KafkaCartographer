#!/bin/bash

show_menu() {
    echo "=============================="
    echo "   Kafka Cartographer CLI"
    echo "=============================="
    echo "1. Scan a project"
    echo "2. Visualize results"
    echo "3. Exit"
    echo "=============================="
    echo -n "Select an option [1-3]: "
}

# Run the scanner
run_scan() {
    echo ""
    echo "--- Scanning ---"
    
    echo "Building Docker image for scanner..."
    docker build -t kafka-cartographer .
    
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build scanner image."
        return
    fi
    
    echo ""
    default_path="$(pwd)/scan"
    echo -n "Enter the full path to scan (default: $default_path): "
    read scan_path
    
    if [ -z "$scan_path" ]; then
        scan_path="$default_path"
    fi
    
    echo "Scanning: $scan_path"
    docker run --rm -v "$scan_path":/scan kafka-cartographer /scan
    
    echo ""
    if [ -f "$scan_path/topic_map.json" ]; then
        echo "Scan complete! Report generated at: $scan_path/topic_map.json"
        
        # If the scan wasn't in the current dir, copy it here for visualization convenience
        if [ "$scan_path" != "$(pwd)" ]; then
             echo "Copying topic_map.json to current directory for visualization..."
             cp "$scan_path/topic_map.json" .
        fi
    else
        echo "Scan finished, but could not check for report."
    fi
    
    read -p "Press Enter to continue..."
}

# Run the visualization
run_visualization() {
    echo ""
    echo "--- Visualization ---"
    
    if [ ! -f "topic_map.json" ]; then
        echo "Error: topic_map.json not found in the current directory."
        echo "Please run a scan first or ensure the report is in this folder."
        read -p "Press Enter to continue..."
        return
    fi

    echo "Building Docker image for visualization..."
    docker build -f Dockerfile.visualization -t kafka-cartographer-web .
    
    if [ $? -ne 0 ]; then
        echo "Error: Failed to build visualization image."
        return
    fi
    
    echo ""
    echo "Starting visualization server on port 8080..."
    echo "Access the map at: http://localhost:8080"
    echo "Press Ctrl+C to stop the server."
    
    docker run --rm -p 8080:80 kafka-cartographer-web
    
    read -p "Press Enter to continue..."
}

# Main Loop
while true; do
    show_menu
    read choice
    case $choice in
        1)
            run_scan
            ;;
        2)
            run_visualization
            ;;
        3)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo "Invalid option. Please try again."
            sleep 1
            ;;
    esac
    echo ""
done
