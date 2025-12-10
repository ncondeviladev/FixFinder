package com.fixfinder.service.impl;

import com.fixfinder.data.TrabajoDAO;

public class TrabajoServiceImpl implements TrabajoService {

    private final TrabajoDAO trabajoDAO;

    public TrabajoServiceImpl() {
        this.trabajoDAO = new TrabajoDAO();
    }
}
