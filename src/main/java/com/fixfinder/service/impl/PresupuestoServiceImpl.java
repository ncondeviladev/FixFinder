package com.fixfinder.service.impl;

import com.fixfinder.data.PresupuestoDAO;

public class PresupuestoServiceImpl implements PresupuestoService {

    private final PresupuestoDAO presupuestoDAO;

    public PresupuestoServiceImpl() {
        this.presupuestoDAO = new PresupuestoDAO();
    }
}
