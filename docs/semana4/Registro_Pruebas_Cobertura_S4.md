# PETSALUD - Registro de pruebas y cobertura - Semana 4

## Resultados generales

- Validacion completa: 133 pruebas aprobadas, 0 fallos, 0 errores y 0 omitidas.
- Comando de validacion: `.\mvnw.cmd verify`.
- JaCoCo 0.8.12: `All coverage checks have been met`.
- Regla: minimo 80 % de cobertura de lineas por cada servicio seleccionado.
- La ejecucion completa se realizo tras reiniciar `target/jacoco.exec`.

## Cobertura verificada

| Servicio | Lineas cubiertas | Cobertura de lineas | Cobertura de ramas |
|---|---:|---:|---:|
| VentaServiceImpl | 49/50 | 98,0% | 96,4% |
| UsuarioServiceImpl | 19/19 | 100,0% | 100,0% |

## Inventario de pruebas ejecutadas

### VentaServiceImplTest - 19 pruebas

| Metodo JUnit | Escenario verificado | Resultado |
|---|---|---|
| listarTodasLasVentas_admin_devuelveTodas | ADMIN puede listar todas las ventas | Aprobada |
| listarTodasLasVentas_cliente_lanzaAccesoDenegado | CLIENTE no puede listar todas las ventas | Aprobada |
| listarVentasPorUsuario_devuelveLasVentasDelUsuario | Listar ventas de usuario verifica la propiedad | Aprobada |
| obtenerVentaPorId_existe_verificaPropietario | Consultar venta existente verifica la propiedad | Aprobada |
| obtenerVentaPorId_noExiste_devuelveVacio | Consultar venta inexistente devuelve vacio | Aprobada |
| realizarVenta_datosValidos_productosConservanSuProveedor | Los productos de una venta conservan su proveedor | Aprobada |
| realizarVenta_datosValidos_ventaContieneLosProductos | La venta contiene los productos vendidos (relacion Venta-Producto) | Aprobada |
| realizarVenta_datosValidos_ventaVinculadaAlUsuario | La venta queda vinculada al usuario que compra (relacion Venta-Usuario) | Aprobada |
| realizarVenta_dosProductos_descuentaElStock | Cada producto vendido descuenta una unidad de su stock | Aprobada |
| realizarVenta_dosProductos_totalEsLaSumaDeLosPrecios | El total de la venta es la suma de los precios de los productos | Aprobada |
| realizarVenta_productoSinStock_lanzaError | No registra la venta si un producto no tiene stock | Aprobada |
| realizarVenta_rolAdmin_registraLaVenta | Un usuario con rol ADMIN puede realizar ventas | Aprobada |
| realizarVenta_rolVeterinario_lanzaAccesoDenegado | Un usuario con rol VETERINARIO no puede realizar ventas | Aprobada |
| realizarVenta_segundoProductoSinStock_noDescuentaPrimero | Si el segundo producto no tiene stock, el primero conserva sus existencias | Aprobada |
| realizarVenta_stockNulo_lanzaError | Un producto con stock nulo impide la venta y cualquier descuento | Aprobada |
| realizarVenta_usuarioSinApellido_lanzaError | No registra la venta si el usuario no tiene apellido | Aprobada |
| realizarVenta_usuarioSinDireccion_lanzaError | No registra la venta si el usuario no tiene direccion | Aprobada |
| realizarVenta_usuarioSinEmail_lanzaError | No registra la venta si el usuario no tiene email | Aprobada |
| realizarVenta_usuarioSinNombre_lanzaError | No registra la venta si el usuario no tiene nombre | Aprobada |

### UsuarioServiceImplTest - 11 pruebas

| Metodo JUnit | Escenario verificado | Resultado |
|---|---|---|
| actualizarUsuario_existente_actualizaSoloDatosPermitidos | Actualiza datos personales sin modificar email, password ni rol | Aprobada |
| actualizarUsuario_inexistente_lanzaExcepcion | No actualiza ni guarda un usuario inexistente | Aprobada |
| eliminarUsuario_idValido_invocaRepositorio | Eliminar usuario delega la eliminacion al repositorio | Aprobada |
| listarUsuarios_conRegistros_devuelveLista | Lista todos los usuarios existentes | Aprobada |
| listarUsuarios_sinRegistros_devuelveListaVacia | Devuelve lista vacia cuando no hay usuarios | Aprobada |
| obtenerUsuarioPorEmail_existente_devuelveUsuario | Busca correctamente un usuario existente por correo | Aprobada |
| obtenerUsuarioPorEmail_inexistente_devuelveVacio | Devuelve Optional vacio si el correo no existe | Aprobada |
| obtenerUsuarioPorId_existente_devuelveUsuario | Busca correctamente un usuario existente por ID | Aprobada |
| obtenerUsuarioPorId_inexistente_devuelveVacio | Devuelve Optional vacio si el ID no existe | Aprobada |
| registrarUsuario_datosValidos_asignaClienteYCodificaPassword | Registro publico asigna CLIENTE y codifica la contrasena | Aprobada |
| registrarUsuario_emailDuplicado_rechazaRegistro | No registra un usuario si el correo ya existe | Aprobada |

### VentaRollbackIntegrationTest - 1 prueba

| Metodo JUnit | Escenario verificado | Resultado |
|---|---|---|
| errorAlGuardarVenta_revierteDescuentosEnBaseDeDatos | errorAlGuardarVenta_revierteDescuentosEnBaseDeDatos | Aprobada |

## Evidencias

- S4_E01_Rollback_Venta_H2.png
- S4_E03_JaCoCo_Cobertura_Original_Semana3.png
- S4_E04_JaCoCo_Servicios_Originales.png
- S4_E06_Verify_133_Pruebas_JaCoCo_80.png
- S4_E07_JaCoCo_Cobertura_Final_Servicios.png

**Nota metodológica:** El registro consolida las pruebas desarrolladas y ampliadas durante la Semana 4, incluyendo escenarios previamente implementados y nuevas pruebas de validación, autorización e integridad transaccional. Los porcentajes de cobertura corresponden a la ejecución conjunta de las pruebas, según los resultados obtenidos mediante JaCoCo, y no a la contribución individual de cada caso.
