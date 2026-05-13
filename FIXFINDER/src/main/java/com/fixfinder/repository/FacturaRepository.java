package com.fixfinder.repository;

import com.fixfinder.modelos.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, Integer> {
    Factura findByTrabajoId(int trabajoId);
}
