CREATE DATABASE MarketControl
GO

USE MarketControl
GO

-- ==========================================
-- TABLAS PADRE (Sin dependencias)
-- ==========================================

CREATE TABLE Categorias(
	IdCategoria int IDENTITY(1,1) NOT NULL,
	NombreCategoria varchar(100) NOT NULL UNIQUE,
	Descripcion varchar(250) NULL,
	Estado bit DEFAULT ((1)),
PRIMARY KEY CLUSTERED (IdCategoria ASC)
) ON [PRIMARY]
GO

CREATE TABLE Clientes(
	IdCliente int IDENTITY(1,1) NOT NULL,
	Nombres varchar(120) NOT NULL,
	Telefono varchar(100) NULL,
	TelefonoHash char(64) NULL,
	Direccion varchar(500) NULL,
	Referencia varchar(500) NULL,
	FechaRegistro datetime DEFAULT (getdate()),
PRIMARY KEY CLUSTERED (IdCliente ASC)
) ON [PRIMARY]
GO

CREATE TABLE Repartidores(
	IdRepartidor int IDENTITY(1,1) NOT NULL,
	Nombres varchar(120) NOT NULL,
	Telefono varchar(100) NULL,
	Estado varchar(20) DEFAULT ('Disponible'),
PRIMARY KEY CLUSTERED (IdRepartidor ASC)
) ON [PRIMARY]
GO

CREATE TABLE Usuarios(
	IdUsuario int IDENTITY(1,1) NOT NULL,
	Nombres varchar(100) NOT NULL,
	Apellidos varchar(100) NOT NULL,
	DNI varchar(100) NOT NULL,
	DniHash char(64) NOT NULL UNIQUE,
	Usuario varchar(50) NOT NULL UNIQUE,
	Email varchar(255) NULL,
	EmailHash char(64) NULL,
	ContrasenaHash varchar(255) NOT NULL,
	Rol varchar(20) NOT NULL,
	Estado bit DEFAULT ((1)),
	FechaRegistro datetime DEFAULT (getdate()),
PRIMARY KEY CLUSTERED (IdUsuario ASC)
) ON [PRIMARY]
GO

-- UNIQUE sobre EmailHash (permite NULLs), no sobre Email cifrado.
CREATE UNIQUE NONCLUSTERED INDEX UQ_UsuarioEmail ON Usuarios (EmailHash ASC) WHERE (EmailHash IS NOT NULL)
GO

INSERT INTO Usuarios (Nombres, Apellidos, DNI, DniHash, Usuario, Email, EmailHash, ContrasenaHash, Rol, Estado)
VALUES ('Administrador', 'Tienda', 'J+LpO3dFyXKCmO3dUp4I9FHIaGVLviftZZ02VlNOT6e9DwKr',
	'c905f2e889983b1c8a045e2b31a7bf75b6a08d504674391ef5f5d594d9662503', 'admin',
	'YONXynkDGqc7Yg4vb+P9qcu8CRfbgdJR0xyhfVLAdDoTifWuO9y6nf27balhYiC18RlbNA==',
	'3e0fe946fc22f6a56ad3623c4d69e31e209622df902f10d96320cecb45e10b88',
	'PBKDF2:210000:2LUEvaSGN+xNS6pYgAnnlQ==:2HD3XitMwJinS0c3HOiPI3dNGBI9CGJCJRokp8yNUBk=', 'Administrador', 1);
GO

-- ==========================================
-- TABLAS HIJAS
-- ==========================================

CREATE TABLE Productos(
	IdProducto int IDENTITY(1,1) NOT NULL,
	IdCategoria int NOT NULL FOREIGN KEY REFERENCES Categorias (IdCategoria),
	CodigoSKU varchar(30) NOT NULL UNIQUE,
	NombreProducto varchar(150) NOT NULL,
	Descripcion varchar(max) NULL,
	Marca varchar(100) NULL,
	ImagenUrl varchar(500) NULL,
	PrecioCompra decimal(10, 2) NOT NULL CHECK (PrecioCompra>(0)),
	PrecioVenta decimal(10, 2) NOT NULL CHECK (PrecioVenta>(0)),
	PorcentajeDescuento decimal(5, 2) DEFAULT ((0)),
	StockActual int NOT NULL DEFAULT ((0)) CHECK (StockActual>=(0)),
	StockMinimo int NOT NULL DEFAULT ((5)),
	FechaRegistro datetime DEFAULT (getdate()),
	Estado bit DEFAULT ((1)),
PRIMARY KEY CLUSTERED (IdProducto ASC)
) ON [PRIMARY]
GO

CREATE TABLE Ventas(
	IdVenta int IDENTITY(1,1) NOT NULL,
	IdUsuario int NOT NULL FOREIGN KEY REFERENCES Usuarios (IdUsuario),
	FechaVenta datetime DEFAULT (getdate()),
	Total decimal(12, 2) NOT NULL,
	MetodoPago varchar(30) NULL,
	Estado varchar(20) DEFAULT ('Completada'),
PRIMARY KEY CLUSTERED (IdVenta ASC)
) ON [PRIMARY]
GO

CREATE TABLE Pedidos(
	IdPedido int IDENTITY(1,1) NOT NULL,
	IdCliente int NOT NULL FOREIGN KEY REFERENCES Clientes (IdCliente),
	IdRepartidor int NULL FOREIGN KEY REFERENCES Repartidores (IdRepartidor),
	FechaPedido datetime DEFAULT (getdate()),
	Estado varchar(20) DEFAULT ('Pendiente'),
	Total decimal(12, 2) NULL,
	TipoEntrega VARCHAR(15) NOT NULL DEFAULT ('Delivery'),
	Observaciones varchar(300) NULL,
	CostoEnvio DECIMAL(10,2) NOT NULL DEFAULT 0,
	MetodoPago varchar(20) NULL,
PRIMARY KEY CLUSTERED (IdPedido ASC)
) ON [PRIMARY]
GO

CREATE TABLE ConfiguracionNegocio(
    Clave varchar(50) NOT NULL PRIMARY KEY,
    Valor varchar(250) NULL
);
GO

INSERT INTO ConfiguracionNegocio (Clave, Valor)
VALUES
('nombre_negocio', 'Bodega el Progreso');
GO

-- ==========================================
-- TABLAS NIETAS (Detalles y Movimientos)
-- ==========================================

CREATE TABLE DetallePedidos(
	IdDetallePedido int IDENTITY(1,1) NOT NULL,
	IdPedido int NOT NULL FOREIGN KEY REFERENCES Pedidos (IdPedido),
	IdProducto int NOT NULL FOREIGN KEY REFERENCES Productos (IdProducto),
	Cantidad int NOT NULL,
	PrecioUnitario decimal(10, 2) NULL,
	Subtotal decimal(10, 2) NULL,
PRIMARY KEY CLUSTERED (IdDetallePedido ASC)
) ON [PRIMARY]
GO

CREATE TABLE DetalleVentas(
	IdDetalleVenta int IDENTITY(1,1) NOT NULL,
	IdVenta int NOT NULL FOREIGN KEY REFERENCES Ventas (IdVenta),
	IdProducto int NOT NULL FOREIGN KEY REFERENCES Productos (IdProducto),
	Cantidad int NOT NULL CHECK (Cantidad>(0)),
	PrecioUnitario decimal(10, 2) NOT NULL,
	Subtotal decimal(10, 2) NOT NULL,
PRIMARY KEY CLUSTERED (IdDetalleVenta ASC)
) ON [PRIMARY]
GO

CREATE TABLE MovimientosInventario(
	IdMovimiento int IDENTITY(1,1) NOT NULL,
	IdProducto int NOT NULL FOREIGN KEY REFERENCES Productos (IdProducto),
	TipoMovimiento varchar(20) NULL,
	Cantidad int NOT NULL,
	FechaMovimiento datetime DEFAULT (getdate()),
	StockResultante int NULL,
	Motivo varchar(250) NULL,
	IdUsuario int NULL FOREIGN KEY REFERENCES Usuarios (IdUsuario),
PRIMARY KEY CLUSTERED (IdMovimiento ASC)
) ON [PRIMARY]
GO

-- ==========================================
-- �NDICES NONCLUSTERED DE RENDIMIENTO
-- ==========================================
CREATE NONCLUSTERED INDEX IX_ClienteTelefono ON Clientes (TelefonoHash ASC)
GO
CREATE NONCLUSTERED INDEX IX_ProductoNombre ON Productos (NombreProducto ASC)
GO
CREATE NONCLUSTERED INDEX IX_PedidoEstado ON Pedidos (Estado ASC)
GO
CREATE NONCLUSTERED INDEX IX_VentasFecha ON Ventas (FechaVenta ASC)
GO
