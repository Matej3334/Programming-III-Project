package org.example;

import java.io.Serializable;

public record WasteSite (double capacity, double la, double lo) implements Serializable{}
