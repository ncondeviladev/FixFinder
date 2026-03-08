package com.fixfinder.ui.dashboard;

/**
 * Clase contenedora que permite lanzar el nuevo Dashboard Principal
 * directamente desde el botón "Run / Play" de IDEs (IntelliJ, VS Code, Eclipse)
 * sin problemas de módulos o rutas.
 */
public class LanzadorDashboard {
    public static void main(String[] args) {
        AppDashboardPrincipal.main(args);
    }
}
