package org.example;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private double la;
    private double lo;
    private List<WasteSite> wasteSiteList;

    public Cluster(double la, double lo){
        this.la = la;
        this.lo = lo;
        this.wasteSiteList = new ArrayList<>();
    }

    public double getLa() {
        return la;
    }


    public double getLo() {
        return lo;
    }

    public void setLa(double la) {
        this.la = la;
    }

    public void setLo(double lo) {
        this.lo = lo;
    }

    public List<WasteSite>  getWasteSiteList() {
        return List.copyOf(wasteSiteList);
    }

    public void clearWasteSiteList(){
        this.wasteSiteList.clear();
    }

    public void addWasteSite(WasteSite wasteSite) {
        this.wasteSiteList.add(wasteSite);
    }

    public double[] changeCenter(){
        if(wasteSiteList.isEmpty()){
            return null;
        }

        double la = 0;
        double lo = 0;
        int length = wasteSiteList.size();
        for(WasteSite wasteSite : wasteSiteList){
            la+=wasteSite.la();
            lo+=wasteSite.lo();
        }

        double[] result = new double[2];
        result[0] = la/length;
        result[1] = lo/length;

        return result;
    }

    //used in parallel

    public synchronized void removeWasteSiteParallel(WasteSite wasteSite){this.wasteSiteList.remove(wasteSite);}
    public synchronized void addWasteSiteParallel(WasteSite wasteSite){this.wasteSiteList.add(wasteSite);}

    public List<WasteSite>  getWasteSiteListCopy() {
        return List.copyOf(wasteSiteList);
    }
}
