package com.fixfinder.dao;

import com.fixfinder.utilidades.DataAccessException;
import java.util.List;

/**
 * Interfaz genérica que define las operaciones CRUD básicas.
 *
 * @param <T> Tipo de la entidad (Usuario, Trabajo, etc.)
 */
public interface BaseDAO<T> {

    /**
     * Inserta un nuevo registro en la base de datos.
     *
     * @param t Objeto a guardar.
     * @throws DataAccessException Si ocurre un error SQL.
     */
    void insertar(T t) throws DataAccessException;

    /**
     * Actualiza un registro existente.
     *
     * @param t Objeto con los datos modificados.
     * @throws DataAccessException Si ocurre un error SQL.
     */
    void actualizar(T t) throws DataAccessException;

    /**
     * Elimina un registro por su ID.
     *
     * @param id Identificador único.
     * @throws DataAccessException Si ocurre un error SQL.
     */
    void eliminar(int id) throws DataAccessException;

    /**
     * Busca un registro por su ID.
     *
     * @param id Identificador único.
     * @return El objeto encontrado o null si no existe.
     * @throws DataAccessException Si ocurre un error SQL.
     */
    T obtenerPorId(int id) throws DataAccessException;

    /**
     * Recupera todos los registros de la tabla.
     *
     * @return Lista con todos los objetos.
     * @throws DataAccessException Si ocurre un error SQL.
     */
    List<T> obtenerTodos() throws DataAccessException;
}
