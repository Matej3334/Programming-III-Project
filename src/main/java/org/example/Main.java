package org.example;


import java.util.List;
import java.util.Scanner;
public class Main {
    private static final String file_path = "src/main/resources/germany/germany.json";
    private static int width=800;
    private static int height=600;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Do you want to use GUI?");
        System.out.println("Select 1 for yes or 2 for no");
        int useGUI = scanner.nextInt();

        System.out.println("Select one of the following");
        System.out.println("1-sequential, 2-parallel, 3-distributed");
        int runProgram = scanner.nextInt();

        System.out.println("Enter number of sites");
        int sites = scanner.nextInt();
        System.out.println("Enter number of clusters");
        int clusters = scanner.nextInt();

        if(useGUI==1){
            System.out.println("Please select width of screen");
            width = scanner.nextInt();
            System.out.println("Please select height of screen");
            height = scanner.nextInt();
        }
        switch (runProgram){
            case 1:
                long start = System.currentTimeMillis();
                Sequential seq = new Sequential(sites, clusters, file_path);
                if(useGUI==1) {
                    List<Cluster> clusterList = seq.getClusterList();
                    GUI gui = new GUI(width, height);
                    gui.drawClusters(clusterList);
                }
                long end = System.currentTimeMillis();
                System.out.println("Time spent computing: " + (end-start) + "ms");
                break;
            case 2:
                System.out.println("Parallel not implemented yet.");
                break;
            case 3:
                System.out.println("Distributed not implemented yet.");
                break;
            default:
                System.out.println("Please Select a valid option");
                break;
        }
    }
}