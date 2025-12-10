package com.fixfinder.service.impl;

import com.fixfinder.data.EmpresaDAO;

public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaDAO empresaDAO;

    public EmpresaServiceImpl() {
        this.empresaDAO = new EmpresaDAO();
    }
}
