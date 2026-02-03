package org.example;

public class EuclideanDistance {

    public static double calculate(double la1, double lo1, double la2, double lo2){
        return Math.sqrt(Math.pow((la1 - la2),2) + Math.pow((lo1 - lo2),2));
    }

}
