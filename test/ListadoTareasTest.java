import models.Tarea;
import models.TareaDAO;
import models.Usuario;
import models.UsuarioDAO;
import org.dbunit.JndiDatabaseTester;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.junit.*;
import play.Logger;
import play.db.Database;
import play.db.Databases;
import play.db.jpa.JPA;
import play.db.jpa.JPAApi;
import services.TareasService;

import java.io.FileInputStream;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class ListadoTareasTest {

    static Database db;
    static JPAApi jpa;
    JndiDatabaseTester databaseTester;

    @BeforeClass
    static public void initDatabase() {
        db = Databases.inMemoryWith("jndiName", "DefaultDS");
        // Necesario para inicializar el nombre JNDI de la BD
        db.getConnection();
        // Se activa la compatibilidad MySQL en la BD H2
        db.withConnection(connection -> {
            connection.createStatement().execute("SET MODE MySQL;");
        });
        jpa = JPA.createFor("memoryPersistenceUnit");
    }

    @Before
    public void initData() throws Exception {
        databaseTester = new JndiDatabaseTester("DefaultDS");
        IDataSet initialDataSet = new FlatXmlDataSetBuilder().build(new
                FileInputStream("test/resources/tareas_dataset.xml"));
        databaseTester.setTearDownOperation(DatabaseOperation.DELETE_ALL);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.setDataSet(initialDataSet);
        databaseTester.onSetup();
    }

    @After
    public void clearData() throws Exception {
        databaseTester.onTearDown();
    }

    @AfterClass
    static public void shutdownDatabase() {
        jpa.shutdown();
        db.shutdown();
    }

    @Test
    public void findTareaPorId() {
        jpa.withTransaction(() -> {
            Tarea tarea = TareaDAO.find(1);
            assertThat(tarea.descripcion, equalTo("Preparar el trabajo del tema 1 de biología"));
        });
    }

    @Test
    public void compararTareas() {
        jpa.withTransaction(() -> {
            Tarea tarea1 = TareaDAO.find(1);

            // Creamos una copia de la tarea1
            // (otro objeto con los mismos atributos)
            Tarea tarea2 = tarea1.copy();
            assertEquals(tarea1, tarea2);

            // Creamos una tarea con la misma descripción,
            // pero sin id
            Tarea tarea3 = new Tarea(tarea1.descripcion);
            assertEquals(tarea1, tarea3);
        });
    }

    @Test
    public void obtenerUsuarioDeTarea() {
        jpa.withTransaction(() -> {
            Tarea tarea = TareaDAO.find(1);
            Usuario usuario = UsuarioDAO.find(1);
            assertEquals(tarea.usuario, usuario);
        });
    }

    @Test
    public void obtenerTareasDeUsuario() {
        jpa.withTransaction(() -> {
            Usuario usuario = UsuarioDAO.find(1);
            assertEquals(usuario.tareas.size(), 3);
        });
    }

    @Test
    public void listadoTareasService() {
        jpa.withTransaction(() -> {
            List<Tarea> tareas = TareasService.listaTareasUsuario(1);
            assertEquals(tareas.size(), 3);

            // Comprobamos que las tareas se devuelven ordenadas por id

            //Se refactoriza la función
            Tarea anterior = tareas.get(0);
            for (int i = 1; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                if (anterior != null) {
                    assertTrue(anterior.id < t.id);
                    anterior = t;
                }
            }
        });
    }

    @Test
    public void busquedaTareasDAO() {

        //En esta transacción comprobamos que se obtienen tres tareas si se busca
        //por la palabra "el".
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareaDAO.busquedaTareasUsuario(1, "el", "id", "asc", 0, 10);
            assertEquals(tareas.size(), 3);

            // Comprobamos que las tareas contienen el valor buscado (actualmente solo por descripción es posible buscar)

            for (int i = 1; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                assertTrue(t.descripcion.contains("el"));
            }
        });

        //En esta transacción comprobamos que se obtienen una tarea si se busca
        //por la palabra "tema".
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareaDAO.busquedaTareasUsuario(1, "tema", "id", "asc", 0, 10);
            assertEquals(tareas.size(), 1);

            // Comprobamos que las tareas contienen el valor buscado (actualmente solo por descripción es posible buscar)

            for (int i = 1; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                assertTrue(t.descripcion.contains("tema"));
            }
        });
    }

    @Test
    public void filtradoTareasDAO() {

        //En esta transacción comprobamos que se obtienen tres tareas si se busca
        //por la palabra "2016". (debe dar true la comprobación de fecha de finalización)
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareaDAO.busquedaTareasUsuario(1, "2016", "id", "asc", 0, 10);
            assertEquals(tareas.size(), 3);

            // Comprobamos que las tareas contienen el valor buscado

            for (int i = 0; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                assertTrue(t.fechaFinTarea.toString().contains("2016"));
            }
        });

        //En esta transacción comprobamos que se obtiene una tarea si se busca
        //por la palabra "Iniciada". (dará true el estado)
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareaDAO.busquedaTareasUsuario(2, "Iniciada", "id", "asc", 0, 10);
            assertEquals(tareas.size(), 1);


            // Comprobamos que las tareas contienen el valor buscado
            for (int i = 0; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                assertTrue(t.estado == 2); //Estado 'Iniciada'
            }
        });

        //En esta transacción comprobamos que se obtiene una tarea si se busca
        //por la palabra "55". (dará true el color)
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareaDAO.busquedaTareasUsuario(1, "55", "id", "asc", 0, 10);
            assertEquals(tareas.size(), 1);

            // Comprobamos que las tareas contienen el valor buscado (actualmente solo por descripción es posible buscar)

            for (int i = 0; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                assertTrue(t.color.contains("55"));
            }
        });
    }

    @Test
    public void busquedaTareasService() {

        //En esta transacción comprobamos que se obtienen tres tareas si se busca
        //por la palabra "el". Los registros se devuelven ordenadas por id ascendentemente
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareasService.busquedaTareasUsuario(1, "el", "id", "asc", 0, 10);
            assertEquals(tareas.size(), 3);

            // Comprobamos que las tareas se devuelven ordenadas por id

            //Se refactoriza la función
            Tarea anterior = tareas.get(0);
            for (int i = 1; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                if (anterior != null) {
                    assertTrue(anterior.id < t.id);
                    anterior = t;
                }
            }
        });

        //En esta transacción comprobamos que se obtienen tres tareas si se busca
        //por la palabra "el". Los registros se devuelven ordenadas por id descendentemente
        jpa.withTransaction(() -> {

            //IdUsuario, Palabra búsqueda, Campo orden, ordenación, inicio, fin
            List<Tarea> tareas = TareasService.busquedaTareasUsuario(1, "el", "id", "desc", 0, 10);
            assertEquals(tareas.size(), 3);

            // Comprobamos que las tareas se devuelven ordenadas descendetemente por id

            //Se refactoriza la función
            Tarea anterior = tareas.get(0);
            for (int i = 1; i < tareas.size(); i++) {
                Tarea t = tareas.get(i);
                if (anterior != null) {
                    assertTrue(anterior.id > t.id);
                    anterior = t;
                }
            }
        });
    }



}
