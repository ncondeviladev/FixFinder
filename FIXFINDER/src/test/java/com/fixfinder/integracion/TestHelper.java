package com.fixfinder.integracion;

import com.fixfinder.modelos.*;
import com.fixfinder.modelos.enums.CategoriaServicio;
import com.fixfinder.modelos.enums.Rol;
import com.fixfinder.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

@Component
public class TestHelper {

    @Autowired private OperarioRepository operarioRepository;
    @Autowired private EmpresaRepository empresaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private TrabajoRepository trabajoRepository;
    @Autowired private PresupuestoRepository presupuestoRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private FacturaRepository facturaRepository;

    private final Random random = new Random();

    // Rastreo de basura generado por el helper
    private final List<Integer> usuariosCreados = new ArrayList<>();
    private final List<Integer> empresasCreadas = new ArrayList<>();
    private final List<Integer> trabajosCreados = new ArrayList<>();
    private final List<Integer> presupuestosCreados = new ArrayList<>();

    public String generarDniUnico() {
        return (10000000 + random.nextInt(89999999)) + "T";
    }

    public String generarCifUnico() {
        return "B" + (10000000 + random.nextInt(89999999));
    }

    public String generarEmailUnico() {
        return "test_" + System.nanoTime() + "@fixfinder.com";
    }

    public void registrarUsuario(int id) {
        if (!usuariosCreados.contains(id)) usuariosCreados.add(id);
    }

    public void registrarEmpresa(int id) {
        if (!empresasCreadas.contains(id)) empresasCreadas.add(id);
    }

    public void registrarTrabajo(int id) {
        if (!trabajosCreados.contains(id)) trabajosCreados.add(id);
    }

    public void registrarPresupuesto(int id) {
        if (!presupuestosCreados.contains(id)) presupuestosCreados.add(id);
    }

    @Transactional
    public void limpiarTodoLoGenerado() {
        presupuestosCreados.forEach(id -> presupuestoRepository.deleteById(id));
        trabajosCreados.forEach(id -> trabajoRepository.deleteById(id));
        usuariosCreados.forEach(id -> usuarioRepository.deleteById(id));
        empresasCreadas.forEach(id -> empresaRepository.deleteById(id));
        
        usuariosCreados.clear();
        empresasCreadas.clear();
        trabajosCreados.clear();
        presupuestosCreados.clear();
    }

    public void limpiarSurgicamente(int idEmpresa) {
        // En JPA/Hibernate con orphanRemoval o CascadeType.ALL esto suele ser automático,
        // pero podemos implementarlo si es necesario.
        empresaRepository.deleteById(idEmpresa);
    }
}
