package com.fixfinder.service.impl;

import com.fixfinder.data.FacturaDAO;

public class FacturaServiceImpl implements FacturaService {

    private final FacturaDAO facturaDAO;

    public FacturaServiceImpl() {
        this.facturaDAO = new FacturaDAO();
    }
}
