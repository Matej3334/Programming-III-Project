package Distributed;


import mpi.MPI;
import org.example.Cluster;
import org.example.EuclideanDistance;
import org.example.JSONReader;
import org.example.WasteSite;


import java.io.IOException;
import java.util.*;


public class MPI_MAIN {
    private static final String file_path = "src/main/resources/germany/germany.json";
    private static final int width = 800;
    private static final int height = 600;

    public static void main(String[] args) throws IOException {
        if(args.length !=4){
            System.out.println("Need 4 arguments to continue: The MPI.Size, " +
                    "number of waste sites, number of clusters and do you want GUI (1 for yes, 0 for no)");
            System.exit(1);
        }
        System.out.println(width+height);

        MPI.Init(new String[]{args[0]});

        int sites = Integer.parseInt(args[1]);
        int clusters = Integer.parseInt(args[2]);
        int useGUI = Integer.parseInt(args[3]);

        int rank = MPI.COMM_WORLD.Rank();
        System.out.println(rank);
        int size = MPI.COMM_WORLD.Size();
        final int root = 0;

        if (rank == root) {

            List<WasteSite> wasteSiteList = initializeWasteSiteList(sites, clusters);
            List<Cluster> clusterList = initializeClusters(wasteSiteList, clusters);
            runRoot(wasteSiteList,clusterList,sites,clusters,size);
        } else {

        }
        MPI.Finalize();
    }

    private static List<WasteSite> initializeWasteSiteList(int sites, int clusters){
        JSONReader jsonReader = new JSONReader();
        List<WasteSite> wasteSiteList = jsonReader.GetList(file_path);

        //Checking if the List is empty
        if(wasteSiteList.isEmpty()){
            System.out.println("Can't get waste site data");
            System.exit(1);
        }

        int siteNum = wasteSiteList.size(); //Getting the size of the List

        if(clusters > sites){
            System.out.println("Number of clusters cannot be larger than number of sites");
            System.exit(1);
        }

        if (sites > siteNum){ //If the number of sites that was requested is larger than dataset, create random sites
            int newSites = sites - siteNum;
            Random random = new Random(2);
            for (int i = 0; i < newSites; i++) {
                double capacity = random.nextDouble(1,180);
                double la = random.nextDouble(47.8, 53.2);
                double lo = random.nextDouble(5.9, 14.2);
                wasteSiteList.add(new WasteSite(capacity, la, lo));
            }
        } else if (sites < siteNum) { //if the number of sites is smaller than dataset, create a smaller ArrayList
            wasteSiteList = new ArrayList<>(wasteSiteList.subList(0, sites));
        }

        return wasteSiteList;
    }


    public static List<Cluster> initializeClusters(List<WasteSite> wasteSiteList, int clusters){
        List<Cluster> clusterList = new ArrayList<>();
        Random random = new Random(1);
        Set<Integer> startClusters = new HashSet<>();

        int ListSize = wasteSiteList.size();
        //Creating a set of random indexes
        while (startClusters.size() < clusters){
            startClusters.add(random.nextInt(ListSize));
        }

        //Assigning random WasteSites as Clusters in ClusterList
        for (Integer index : startClusters){
            WasteSite cluster = wasteSiteList.get(index);
            clusterList.add(new Cluster(cluster.la(),cluster.lo()));
        }

        return clusterList;
    }


    private static void runRoot(List<WasteSite> wasteSiteList, List<Cluster> clusterList, int sites, int clusters, int size) throws IOException {
        assignClusters(wasteSiteList,clusterList);

        boolean change = true;
        int iterations = 1;

        Object[] sendBuffer = new Object[size];
        for (int i = 0; i < size; i++) {
            sendBuffer[i] = new ArrayList<Cluster>();
        }

        for (int i = 0; i < clusterList.size(); i++) {
            int targetRank = i % size;
            ((List<Cluster>) sendBuffer[targetRank]).add(clusterList.get(i));
        }

        while(change && iterations < 20) {
            change = false;

            Object[] receivedBuffer = new Object[1];
            MPI.COMM_WORLD.Scatter(sendBuffer, 0, 1, MPI.OBJECT,
                    receivedBuffer, 0, 1, MPI.OBJECT, 0);

            List<Cluster> rootClusters = (List<Cluster>) receivedBuffer[0];

            if(change==false){
                break;
            }


            for(WasteSite wasteSite : wasteSiteList){
                Cluster nearestCluster = null;
                double minDistance = Double.MAX_VALUE;

                for(Cluster cluster : clusterList){
                    double distance = EuclideanDistance.calculate(
                            wasteSite.la(), wasteSite.lo(), cluster.getLa(), cluster.getLo());

                    if(distance < minDistance){
                        minDistance = distance;
                        nearestCluster = cluster;
                    }
                }

                assert nearestCluster != null;
                nearestCluster.addWasteSite(wasteSite);
            }

            iterations++;
        }

    }

    private void runWorker(){
        while(true){
            //MPI.COMM_WORLD.

            /*
            double[] newCenter = cluster.changeCenter;
            MPI.COMM_WORLD.SEND
             */
        }
    }

    private static void assignClusters(List<WasteSite> wasteSiteList, List<Cluster> clusterList){
        for(Cluster cluster: clusterList){
            cluster.clearWasteSiteList();
        }

        for(WasteSite wasteSite : wasteSiteList){
            Cluster nearestCluster = null;
            double minDistance = Double.MAX_VALUE;

            for(Cluster cluster : clusterList){
                double distance = EuclideanDistance.calculate(
                        wasteSite.la(), wasteSite.lo(), cluster.getLa(), cluster.getLo());

                if(distance < minDistance){
                    minDistance = distance;
                    nearestCluster = cluster;
                }
            }

            assert nearestCluster != null;
            nearestCluster.addWasteSite(wasteSite);
        }
    }

}
