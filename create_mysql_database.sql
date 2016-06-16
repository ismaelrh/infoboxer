-- Base de datos para guardar stats. Mysql
CREATE DATABASE infoboxer;
-- Base de datos para suggestions
CREATE DATABASE suggestions;

-- Tabla que guarda registros básicos
CREATE TABLE register 
(
 registerId INT NOT NULL AUTO_INCREMENT, -- Identifica el registro en global
 sessionId INT NOT NULL, -- Identifica a la sesion
 timestamp DATETIME NOT NULL, -- Se guarda con precision de 1 segundo
 subject VARCHAR(255) NOT NULL,
 action VARCHAR(255) NOT NULL,
 value VARCHAR(255),
 PRIMARY KEY(registerId)
 
);


-- Tabla que guarda un resumen muy general de tiempos de edicion
CREATE TABLE summary
(

 sessionId INT NOT NULL,
 username VARCHAR(255),
 seconds INT NOT NULL,
 PRIMARY KEY(sessionId)

);

-- Tabla secundaria que guarda infoboxes generados
CREATE TABLE infobox
(
 registerId INT NOT NULL,
 sessionId INT NOT NULL,
 categories VARCHAR(1000),
 pageName VARCHAR(255),
 infoboxCode VARCHAR(10000),
rdfCode VARCHAR(10000),
 FOREIGN KEY(registerId) REFERENCES register(registerId),
 PRIMARY KEY(registerId)

);

-- Tabla secundaria que guarda encuestas
CREATE TABLE survey
(
 registerId INT NOT NULL,
 sessionId INT NOT NULL,
 response1 INT,
 response2 INT,
 response3 INT,
 freeText VARCHAR(1000),
FOREIGN KEY(registerId) REFERENCES register(registerId),
 PRIMARY KEY(registerId)

);

-- Trigger que crea un resumen de tiempo de edicion
DROP TRIGGER IF EXISTS create_summary;
delimiter |

CREATE TRIGGER create_summary BEFORE INSERT ON register
  FOR EACH ROW
  BEGIN
    DECLARE inicio TIMESTAMP; -- Fecha inicio
    DECLARE segundos TIMESTAMP;
    DECLARE username VARCHAR(255);
    
    IF NEW.action = "EDITION END" THEN
		 -- Se ha cerrado la sesion, guardamos datos de resumen
		SET inicio = (SELECT MIN(timestamp) FROM register WHERE sessionId = NEW.sessionId and action="EDITION START");
		SET username = (SELECT value FROM register WHERE sessionId = NEW.sessionId AND action="SESSION OPENED");
		INSERT INTO summary VALUES(NEW.sessionId,username,UNIX_TIMESTAMP(NEW.timestamp) - UNIX_TIMESTAMP(inicio));
    END IF;
  END;
  |
delimiter ;


-- Para guardar tiempos con edición tradicional de infobox en wikimedia
CREATE TABLE wikimediaTime
(
 id INT NOT NULL AUTO_INCREMENT,
 username VARCHAR(255) NOT NULL,
 date DATE NOT NULL,
 infobox VARCHAR(10000) NOT NULL,
 time INT NOT NULL,
 PRIMARY KEY(id)
);
