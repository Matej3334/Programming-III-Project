package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class Cluster {
    private double la;
    private double lo;
    private List<WasteSite> wasteSiteList;
    private List<WasteSite> newSites;

    public Cluster(double la, double lo){
        this.la = la;
        this.lo = lo;
        this.wasteSiteList = new ArrayList<>();
        this.newSites = new ArrayList<>();
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
        return wasteSiteList;
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

    //methods used in parallel

    public synchronized void removeWasteSiteParallel(WasteSite wasteSite){
        wasteSiteList.remove(wasteSite);
    }
    public synchronized void addWasteSiteParallel(WasteSite wasteSite){
        newSites.add(wasteSite);
    }

    public void clearNewWasteSiteList(){
        this.newSites.clear();
    }
    public void addNewSites(){
        for(WasteSite wasteSite: newSites){
            wasteSiteList.add(wasteSite);
        }
        clearNewWasteSiteList();
    }

    public List<WasteSite> getWasteSiteListCopy(){return List.copyOf(wasteSiteList);}
}
