package com.fixfinder.service.impl;

import com.fixfinder.data.OperarioDAO;

public class OperarioServiceImpl implements OperarioService {

    private final OperarioDAO operarioDAO;

    public OperarioServiceImpl() {
        this.operarioDAO = new OperarioDAO();
    }
}
