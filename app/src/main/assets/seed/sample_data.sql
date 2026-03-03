-- Serviaux Sample Data (optional demo data)
-- Loaded only if user chooses "Cargar ejemplos" on first launch

-- ══════════════════════════════════════════════════════════
-- SAMPLE USERS
-- ══════════════════════════════════════════════════════════
INSERT INTO users (name, username, role, passwordHash, commissionType, commissionValue, active, createdAt, updatedAt) VALUES ('NICOLE PLAZA', 'nicopla', 'RECEPCIONISTA', 'YKG9AmWRa9MimaGi+umdjQ==:58efa4184e36d40ce65852f0170907a1c98b2e1b30ec6365efe0208923912ce0', 'NINGUNA', 0.0, 1, 1772486177187, 1772486177187);
INSERT INTO users (name, username, role, passwordHash, commissionType, commissionValue, active, createdAt, updatedAt) VALUES ('SMITH MOSQUERA', 'smimos', 'MECANICO', 'PquMgiIOVNFjPByPGDYjBg==:39edec4a94a57b18bcbba242ee404d4a516f05aab6049dd09cb39b5e713c630c', 'NINGUNA', 0.0, 1, 1772486177187, 1772486177187);
INSERT INTO users (name, username, role, passwordHash, commissionType, commissionValue, active, createdAt, updatedAt) VALUES ('RONNY MOSQUERA', 'marciano', 'MECANICO', 'ReFcYpw3nNWLyuFXIpYijw==:32a70386ad3acc351799f600debe85edca6067b748805597fe906adb7df2f9c0', 'NINGUNA', 0.0, 1, 1772486177187, 1772486177187);

-- ══════════════════════════════════════════════════════════
-- SAMPLE CUSTOMERS
-- ══════════════════════════════════════════════════════════
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (2, 'JUAN CARLOS PEREZ GARCIA', 'CEDULA', '0901234567', '593991234567', NULL, 'AV. 9 DE OCTUBRE 100', NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (3, 'MARIA ELENA RODRIGUEZ LOPEZ', 'CEDULA', '0907654321', '593997654321', NULL, 'CALLE CHILE 200', NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (4, 'PEDRO LUIS MARTINEZ SUAREZ', 'CEDULA', '0912345678', '593982345678', NULL, NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (5, 'ANA LUCIA FERNANDEZ VERA', 'CEDULA', '0918765432', '593988765432', NULL, 'URDESA CENTRAL', NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (6, 'CARLOS ANDRES MORALES PINO', 'CEDULA', '0923456789', '593973456789', NULL, NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (7, 'GABRIELA SOLEDAD CASTRO LOOR', 'CEDULA', '0934567890', '593964567890', NULL, 'SAMBORONDON', NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (8, 'ROBERTO ALEJANDRO NUNEZ LEON', 'CEDULA', '0945678901', '593955678901', NULL, NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (9, 'DIANA PATRICIA HERRERA RIVAS', 'CEDULA', '0956789012', '593946789012', NULL, 'CEIBOS', NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (10, 'FRANCISCO JAVIER SALAZAR MENA', 'CEDULA', '0967890123', '593937890123', NULL, NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO customers (id, fullName, docType, idNumber, phone, email, address, notes, createdAt, updatedAt) VALUES (11, 'LUCIA FERNANDA PAREDES BRAVO', 'CEDULA', '0978901234', '593928901234', NULL, 'VIA A LA COSTA KM 12', NULL, 1772486177187, 1772486177187);

-- ══════════════════════════════════════════════════════════
-- SAMPLE VEHICLES
-- ══════════════════════════════════════════════════════════
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (1, 2, 'GBQ1234', 'CHEVROLET', 'SAIL', 'LS AC', 2019, NULL, 'BLANCO', 'SEDAN', 45000, NULL, NULL, '4X2', 'MANUAL', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (2, 3, 'GMB5678', 'KIA', 'RIO', 'EX AC', 2020, NULL, 'GRIS', 'SEDAN', 32000, NULL, NULL, '4X2', 'AUTOMATICA', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (3, 4, 'GSK9012', 'HYUNDAI', 'TUCSON', 'GL 2.0', 2018, NULL, 'NEGRO', 'SUV', 78000, NULL, NULL, '4X2', 'AUTOMATICA', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (4, 5, 'PDO3456', 'TOYOTA', 'COROLLA', 'XLI 1.6', 2021, NULL, 'PLATEADO', 'SEDAN', 25000, NULL, NULL, '4X2', 'MANUAL', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (5, 6, 'GSU7890', 'CHEVROLET', 'DMAX', 'CD 4X4', 2017, NULL, 'ROJO', 'DOBLE CABINA', 95000, NULL, NULL, '4X4', 'MANUAL', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (6, 7, 'ABC1234', 'NISSAN', 'SENTRA', 'B15 1.8', 2015, NULL, 'AZUL', 'SEDAN', 120000, NULL, NULL, '4X2', 'MANUAL', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (7, 8, 'DEF5678', 'MAZDA', 'CX-5', 'GRAND TOURING', 2022, NULL, 'BLANCO', 'SUV', 15000, NULL, NULL, '4X2', 'AUTOMATICA', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (8, 9, 'GHI9012', 'FORD', 'EXPLORER', 'LIMITED 3.5', 2019, NULL, 'NEGRO', 'SUV', 55000, NULL, NULL, '4X4', 'AUTOMATICA', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (9, 10, 'JKL3456', 'HYUNDAI', 'ACCENT', 'GL 1.4', 2016, NULL, 'DORADO', 'SEDAN', 88000, NULL, NULL, '4X2', 'MANUAL', NULL, NULL, 1772486177187, 1772486177187);
INSERT INTO vehicles (id, customerId, plate, brand, model, version, year, vin, color, vehicleType, currentMileage, engineDisplacement, engineNumber, drivetrain, transmission, notes, photoPaths, createdAt, updatedAt) VALUES (10, 11, 'MNO7890', 'CHEVROLET', 'AVEO', 'EMOTION 1.6', 2014, NULL, 'VERDE', 'SEDAN', 145000, NULL, NULL, '4X2', 'MANUAL', NULL, NULL, 1772486177187, 1772486177187);

-- ══════════════════════════════════════════════════════════
-- SAMPLE PARTS (basic workshop inventory)
-- ══════════════════════════════════════════════════════════
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('REPUESTO GENERICO', 'REPUESTO GENERICO UNIVERSAL', '99999', 'GENERICO', 1.0, 1.0, 500, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('ACEITE 5W-30 SINTETICO', 'GALON ACEITE SINTETICO 5W-30', 'AC001', 'MOBIL', 18.0, 25.0, 50, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('ACEITE 10W-40 SEMI-SINTETICO', 'GALON ACEITE SEMI-SINTETICO 10W-40', 'AC002', 'CASTROL', 14.0, 20.0, 40, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('ACEITE 20W-50 MINERAL', 'GALON ACEITE MINERAL 20W-50', 'AC003', 'SHELL', 10.0, 15.0, 30, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('FILTRO DE ACEITE UNIVERSAL', 'FILTRO ACEITE ROSCA UNIVERSAL', 'FI001', 'MANN-FILTER', 4.0, 7.0, 60, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('FILTRO DE AIRE SEDAN', 'FILTRO AIRE PARA SEDAN GENERICO', 'FI002', 'MAHLE', 5.0, 9.0, 40, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('FILTRO DE GASOLINA', 'FILTRO COMBUSTIBLE EN LINEA', 'FI003', 'BOSCH', 3.0, 6.0, 35, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('FILTRO DE CABINA', 'FILTRO HABITACULO AC', 'FI004', 'MANN-FILTER', 6.0, 12.0, 25, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('PASTILLAS DE FRENO DELANTERAS', 'JUEGO PASTILLAS FRENO DELANTERO', 'FR001', 'BREMBO', 15.0, 28.0, 20, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('PASTILLAS DE FRENO TRASERAS', 'JUEGO PASTILLAS FRENO TRASERO', 'FR002', 'TRW', 12.0, 22.0, 20, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('BUJIAS IRIDIUM', 'JUEGO 4 BUJIAS IRIDIUM', 'BU001', 'NGK', 20.0, 35.0, 30, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('BUJIAS PLATINUM', 'JUEGO 4 BUJIAS PLATINUM', 'BU002', 'DENSO', 16.0, 28.0, 25, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('LIQUIDO DE FRENOS DOT 4', 'BOTELLA 500ML LIQUIDO FRENOS', 'LQ001', 'BOSCH', 4.0, 8.0, 30, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('REFRIGERANTE', 'GALON REFRIGERANTE VERDE', 'LQ002', 'GENERICA', 5.0, 10.0, 20, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('CORREA DE DISTRIBUCION', 'CORREA DISTRIBUCION UNIVERSAL', 'CO001', 'GATES', 18.0, 32.0, 15, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('TENSOR DE CORREA', 'TENSOR CORREA DISTRIBUCION', 'CO002', 'SKF', 22.0, 38.0, 10, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('AMORTIGUADOR DELANTERO', 'AMORTIGUADOR DELANTERO SEDAN', 'SU001', 'MONROE', 35.0, 55.0, 12, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('AMORTIGUADOR TRASERO', 'AMORTIGUADOR TRASERO SEDAN', 'SU002', 'KYB', 30.0, 48.0, 12, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('BATERIA 13 PLACAS', 'BATERIA 12V 13 PLACAS', 'BA001', 'GENERICA', 55.0, 85.0, 8, 1, 1772486177187, 1772486177187);
INSERT INTO parts (name, description, code, brand, unitCost, salePrice, currentStock, active, createdAt, updatedAt) VALUES ('LIMPIA INYECTORES', 'ADITIVO LIMPIEZA DE INYECTORES 350ML', 'AD001', 'GENERICA', 5.0, 10.0, 24, 1, 1772486177187, 1772486177187);

-- ══════════════════════════════════════════════════════════
-- SAMPLE WORK ORDERS
-- ══════════════════════════════════════════════════════════
-- Order 1: Cambio de aceite - LISTO (vehicle 1, customer 2)
INSERT INTO work_orders (id, vehicleId, customerId, entryDate, status, priority, orderType, arrivalCondition, customerComplaint, initialDiagnosis, assignedMechanicId, entryMileage, fuelLevel, checklistNotes, totalLabor, totalParts, total, photoPaths, filePaths, deliveryNote, invoiceNumber, notes, createdBy, updatedBy, createdAt, updatedAt) VALUES (1, 1, 2, 1772486177187, 'LISTO', 'MEDIA', 'SERVICIO_NUEVO', 'RODANDO', 'CAMBIO DE ACEITE Y FILTRO', 'CAMBIO DE ACEITE Y FILTRO', NULL, 45000, '3/4', NULL, 5.0, 32.0, 37.0, NULL, NULL, NULL, NULL, NULL, 1, 1, 1772486177187, 1772486177187);

-- Order 2: Frenos - EN_PROCESO (vehicle 3, customer 4)
INSERT INTO work_orders (id, vehicleId, customerId, entryDate, status, priority, orderType, arrivalCondition, customerComplaint, initialDiagnosis, assignedMechanicId, entryMileage, fuelLevel, checklistNotes, totalLabor, totalParts, total, photoPaths, filePaths, deliveryNote, invoiceNumber, notes, createdBy, updatedBy, createdAt, updatedAt) VALUES (2, 3, 4, 1772486177187, 'EN_PROCESO', 'ALTA', 'SERVICIO_NUEVO', 'RODANDO', 'RUIDO AL FRENAR', 'PASTILLAS DE FRENO DESGASTADAS', NULL, 78000, '1/2', NULL, 25.0, 28.0, 53.0, NULL, NULL, NULL, NULL, NULL, 1, 1, 1772486177187, 1772486177187);

-- Order 3: Mantenimiento completo - RECIBIDO (vehicle 5, customer 6)
INSERT INTO work_orders (id, vehicleId, customerId, entryDate, status, priority, orderType, arrivalCondition, customerComplaint, initialDiagnosis, assignedMechanicId, entryMileage, fuelLevel, checklistNotes, totalLabor, totalParts, total, photoPaths, filePaths, deliveryNote, invoiceNumber, notes, createdBy, updatedBy, createdAt, updatedAt) VALUES (3, 5, 6, 1772486177187, 'RECIBIDO', 'MEDIA', 'SERVICIO_NUEVO', 'RODANDO', 'MANTENIMIENTO 90000 KMS', NULL, NULL, 95000, '1/4', NULL, 0.0, 0.0, 0.0, NULL, NULL, NULL, NULL, NULL, 1, 1, 1772486177187, 1772486177187);

-- ══════════════════════════════════════════════════════════
-- SERVICE LINES (for orders 1 and 2)
-- ══════════════════════════════════════════════════════════
-- Order 1: Cambio aceite
INSERT INTO service_lines (workOrderId, description, laborCost, discount, notes, createdAt, updatedAt) VALUES (1, 'CAMBIO DE ACEITE Y FILTRO', 5.0, 0.0, NULL, 1772486177187, 1772486177187);
-- Order 2: Frenos
INSERT INTO service_lines (workOrderId, description, laborCost, discount, notes, createdAt, updatedAt) VALUES (2, 'CAMBIO DE PASTILLAS DE FRENO DELANTERAS', 25.0, 0.0, NULL, 1772486177187, 1772486177187);

-- ══════════════════════════════════════════════════════════
-- WORK ORDER MECHANICS (mechanic assignments)
-- ══════════════════════════════════════════════════════════
-- User IDs: smimos=3, marciano=4 (admin=1, nicopla=2)
-- Order 1: smimos assigned
INSERT INTO work_order_mechanics (workOrderId, mechanicId, commissionType, commissionValue, commissionAmount, commissionPaid, paidAt, createdAt) VALUES (1, 3, 'PORCENTAJE', 10.0, 3.7, 0, NULL, 1772486177187);
-- Order 2: marciano assigned
INSERT INTO work_order_mechanics (workOrderId, mechanicId, commissionType, commissionValue, commissionAmount, commissionPaid, paidAt, createdAt) VALUES (2, 4, 'PORCENTAJE', 10.0, 5.3, 0, NULL, 1772486177187);
