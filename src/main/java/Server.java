import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exception.DaoException;
import model.*;
import model.Calendar;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.PostgresQuirks;
import persistence.Sql2oAvailabilityDao;
import persistence.Sql2oConnectionDao;
import persistence.Sql2oCalendarDao;
import persistence.Sql2oEventDao;
import persistence.Sql2oUserDao;
import security.Encryption;
import static spark.Spark.*;
import static model.SqlSchema.*;

import spark.Spark;
import spark.utils.IOUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.cdimascio.dotenv.Dotenv;

public class Server {

    private static Sql2o sql2o;

    private static Sql2o getSql2o() {
        if(sql2o == null) {
            // create data source - update to use postgresql
            try {
                Properties props = getDbUrl(System.getenv("DATABASE_URL"));
                sql2o = new Sql2o(new HikariDataSource(new HikariConfig(props)), new PostgresQuirks());
                //sql2o = new Sql2o(props.getProperty("jdbcUrl"), props.getProperty("username"), props.getProperty("password"));
            } catch(URISyntaxException | Sql2oException e) {
                e.printStackTrace();
            }

            try (org.sql2o.Connection con = sql2o.beginTransaction()) {
                con.createQuery(UsersSchema).executeUpdate();
                con.createQuery(EventsSchema).executeUpdate();
                con.createQuery(CalendarsSchema).executeUpdate();
                con.createQuery(ConnectionsSchema).executeUpdate();
                con.createQuery(AvailabilitiesSchema).executeUpdate();
                con.commit();
            } catch (Sql2oException e) {
                e.printStackTrace();
            }
        }

        return sql2o;
    }

    private static Properties getDbUrl(String databaseUrl) throws URISyntaxException {
        Properties props = new Properties();
        if (databaseUrl == null) {
            Dotenv dotenv = Dotenv.load();
            props.setProperty("username", dotenv.get("DEV_DB_USER"));
            props.setProperty("password", dotenv.get("DEV_DB_PWORD"));
            props.setProperty("jdbcUrl",  dotenv.get("DEV_DB_URL"));
        } else {
            URI dbUri = new URI(databaseUrl);

            props.setProperty("username", dbUri.getUserInfo().split(":")[0]);
            props.setProperty("password", dbUri.getUserInfo().split(":")[1]);
            props.setProperty("jdbcUrl",  "jdbc:postgresql://" + dbUri.getHost() + ':'
                    + dbUri.getPort() + dbUri.getPath() + "?sslmode=require");
        }

        return props;
    }

    final static int PORT_NUM = 7000;
    private static int getHerokuAssignedPort() {
        String herokuPort = System.getenv("PORT");
        if (herokuPort != null) {
          return Integer.parseInt(herokuPort);
        }
        return PORT_NUM;
    }

    public static void main(String[] args) {
        // set port number
        port(getHerokuAssignedPort());
        
        getSql2o();

        staticFiles.location("/public");

        /* after((Filter) (request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST");
        }); */

        post("/api/login", (req, res) -> {
            Map<String, Object> model = new HashMap<>();
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            boolean correct = new Sql2oUserDao(getSql2o()).checkCred(username, password);
            if (correct) {
                //res.cookie("username", username);
                res.status(200);
            }
            else {
                res.status(401);
            }
            return "";
        });

//        get("/", (req, res) -> {
//            Map<String, Object> model = new HashMap<>();
//            if (req.cookie("username") != null)
//                model.put("username", req.cookie("username"));
//            res.status(200);
//            res.type("text/html");
//            return IOUtils.toString(Spark.class.getResourceAsStream("/public/static/html/index.html"));
//        });

        //adduser route; allows a new user to be added

        post("/api/adduser", (req, res) -> {
            System.out.println("request received!");
            String username = req.queryParams("username");
            String password = req.queryParams("password");
            String salt = Encryption.makeSalt();
            //res.cookie("username", username);
            User user = new User(username, Encryption.sha2_hash(password, salt), salt);
            new Sql2oUserDao(getSql2o()).add(user);
            res.redirect("/");
            //don't return actual new user for security reasons
            //but return empty string nonetheless
            return "";
        });


        // calendars route; return list of calendars as JSON
        get("/api/calendars", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String name = req.cookie("username");
            int userId = new Sql2oUserDao(sql2o).getUserFromName(name).getId();
            String results = new Gson().toJson(new Sql2oCalendarDao(sql2o).listOne(userId));
            res.type("application/json");
            res.status(200);
            return results;
        });
    
        //calendar route; returns availabilities associated with the calendar id
        get("/api/calendar", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String username = req.cookie("username");
            String idParam = req.queryParams("id");
            String results;
            int userId = new Sql2oUserDao(sql2o).getUserFromName(username).getId();
            if(idParam != null) {
                int id = Integer.parseInt(idParam);
                Calendar cal = new Sql2oCalendarDao(sql2o).getCal(id);
                User user = new Sql2oUserDao(sql2o).getUserFromId(cal.getUserId());
                if (!username.equals(user.getName())) {
                    res.status(404);
                    return "";
                }
                List<Availability> availabilities = new Sql2oAvailabilityDao(sql2o).listAllInCal(cal);
                results = new Gson().toJson(AvailableDates.createFromAvailability(user.getName(), cal.getTitle(), availabilities));
            } else {
                String name = req.cookie("username");
                results = new Gson().toJson(new Sql2oCalendarDao(sql2o).listOne(userId));
                System.out.println(results);
            }

            res.type("application/json");
            res.status(200);
            return results;
        });

        get("/api/aggregate", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String results = "";
            //event id
            int eventId = Integer.parseInt(req.queryParams("id"));
            Event event = new Sql2oEventDao(sql2o).getEventFromId(eventId);
            if(eventId > 0) {
                List<Connection> conns = new Sql2oConnectionDao(sql2o).listOneEvent(eventId);
                List<AvailableDates> availDatesList = new ArrayList<>();
                //get all available dates of all calendars associated with event id
                for(Connection con : conns) {
                        Calendar cal = new Sql2oCalendarDao(sql2o).getCal(con.getCalendarId());
                        User user = new Sql2oUserDao(sql2o).getUserFromId(cal.getUserId());
                        List<Availability> availList = new Sql2oAvailabilityDao(sql2o).listAllInCal(cal);
                        AvailableDates availDates = AvailableDates.createFromAvailability(user.getName(), cal.getTitle(), availList);
                        availDatesList.add(availDates);
                }
                results = new Gson().toJson(new AggrAvail(event, availDatesList));
            }
            res.type("application/json");
            res.status(200);
            return results;
        });

        //addcalendar route; add a new calendar
        post("/api/addcalendar", (req, res) -> {
            long startTime = System.nanoTime();
            String title = req.queryParams("title");
            String username = req.cookie("username");
            int userId = new Sql2oUserDao(sql2o).getUserFromName(username).getId();
            Calendar cal = new Calendar(title, userId);
            System.out.println(cal);
            new Sql2oCalendarDao(sql2o).add(cal);
            String blob = req.body();
            System.out.println(blob);
            long stopTime = System.nanoTime();     
            AvailableDates availDates = new Gson().fromJson(blob, AvailableDates.class);
            Sql2oAvailabilityDao availabilityDao = new Sql2oAvailabilityDao(sql2o);
            availabilityDao.addBatch(availDates.getAvailabilityStream(cal.getId()));
            res.status(201);
            res.type("application/json");
            System.out.println(stopTime - startTime);       
            return new Gson().toJson(cal.toString());
        });

        //delcalendar route; delete calendar
        post("/api/delcalendar", (req, res) -> {
            Sql2o sql2o = getSql2o();
            int id = Integer.parseInt(req.queryParams("id"));
            Calendar cal = new Calendar(id);
            try {
                cal = new Sql2oCalendarDao(sql2o).getCal(id);
                System.out.println(cal);
                new Sql2oCalendarDao(sql2o).delete(cal);
                res.status(204);
            } catch (DaoException ex) {
                res.status(404);
            }
            res.type("application/json");
            return new Gson().toJson(cal.toString());
        });

        //events route; list all events
        get("/api/events", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String username = req.cookie("username");
            boolean filter = !"true".equals(req.queryParams("all"));
            Sql2oEventDao eventDao = new Sql2oEventDao(sql2o);
            List<Event> events;
            if(filter) {
                int userId = new Sql2oUserDao(sql2o).getUserFromName(username).getId();
                events = new Sql2oConnectionDao(sql2o).listOneUser(userId).stream()
                        .map(Connection::getEventId)//.distinct()
                        .map(eventDao::getEventFromId)
                        .collect(Collectors.toList());
            }
            else {
                events = eventDao.listAll();
            }
            String results = new Gson().toJson(events);
            res.type("application/json");
            res.status(200);
            return results;
        });

        //delevent route; deletes event
        post("/api/delevent", (req, res) -> {
            int id = Integer.parseInt(req.queryParams("id"));
            Event event = new Event(id);
            try {
                new Sql2oEventDao(getSql2o()).delete(event);
                res.status(204);
            } catch (DaoException ex) {
                res.status(404);
            }
            res.type("application/json");
            return new Gson().toJson(event.toString());
        });

        //addevent route; inserts a new event
        post("/api/addevent", (req, res) -> {
            String title = req.queryParams("title");
            int startTime = Integer.parseInt(req.queryParams("startTime"));
            int endTime = Integer.parseInt(req.queryParams("endTime"));
            Event event = new Event(title, startTime, endTime);
            System.out.println(event.toString());
            new Sql2oEventDao(getSql2o()).add(event);
            res.status(201);
            res.type("application/json");
            return new Gson().toJson(event);
        });

        //addconnection route; associates a calendar and an event together
        post("/api/addconnection", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String username = req.cookie("username");
            int userId = new Sql2oUserDao(sql2o).getUserFromName(username).getId();
            int eventId = Integer.parseInt(req.queryParams("eventId"));
            int calendarId = Integer.parseInt(req.queryParams("calendarId"));
            Connection conn = new Connection(eventId, calendarId, userId);
            System.out.println(conn.toString());
            new Sql2oConnectionDao(sql2o).add(conn);
            res.status(201);
            res.type("application/json");
            return new Gson().toJson(conn.toString());
        });

        //delonnection route; inserts a new event
        post("/api/delconnection", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String username = req.cookie("username");
            int userId = new Sql2oUserDao(sql2o).getUserFromName(username).getId();
            int eventId = Integer.parseInt(req.queryParams("eventId"));
            int calendarId = Integer.parseInt(req.queryParams("calendarId"));
            Connection conn = new Connection(eventId, calendarId, userId);
            System.out.println(conn.toString());
            try {
                new Sql2oConnectionDao(sql2o).delete(conn);
                res.status(204);
            } catch (DaoException e) {
                res.status(404);
            }
            res.type("application/json");
            return new Gson().toJson(conn.toString());
        });

        //connections route; lists all availabilities
        get("/api/connections", (req, res) -> {
            Sql2oConnectionDao sql2OConnectionDao = new Sql2oConnectionDao(getSql2o());
            String results = new Gson().toJson(sql2OConnectionDao.listAll());
            res.type("application/json");
            res.status(200);
            return results;
        });

        //changepassword route; allows user to change password
        post("/api/changepassword", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            String username = req.cookie("username");
            String password = req.queryParams("password");
            String newpassword = req.queryParams("newpassword");
            boolean pcheck = new Sql2oUserDao(getSql2o()).passwordcheck(username, password, newpassword);
            if (pcheck) {
                res.status(200);
            }
            else {
                res.status(400);
            }
            res.type("application/json");
            //don't return actual new user for security reasons
            //but return empty string nonetheless
            return "";
        });

        //updateconnection route; edit the calendar associated with the event
        post("/api/updateconnection", (req, res) -> {
            Sql2o sql2o = getSql2o();
            String username = req.cookie("username");
            int userId = new Sql2oUserDao(sql2o).getUserFromName(username).getId();
            int eventId = Integer.parseInt(req.queryParams("eventId"));
            System.out.println(req.queryParams("calendarId"));
            int calendarId = Integer.parseInt(req.queryParams("calendarId"));
            Connection conn = new Connection(eventId, calendarId, userId);
            System.out.println(conn.toString());
            List<Connection> updateconnections = new Sql2oConnectionDao(sql2o).updateconnectionscheck(eventId, calendarId, userId);
            if (updateconnections.size() == 1) {
                new Sql2oConnectionDao(sql2o).delete(updateconnections.get(0));
            }

            new Sql2oConnectionDao(sql2o).add(conn);
            res.status(201);
            res.type("application/json");
            return new Gson().toJson(conn.toString());
        });

        //users route; lists all users
        get("/users", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            Sql2oUserDao sql2oUserDao = new Sql2oUserDao(getSql2o());
            String results = new Gson().toJson(sql2oUserDao.listAll());
            res.type("application/json");
            res.status(200);
            return results;
        });

        //deluser route; deletes users
        post("/deluser", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            int id = Integer.parseInt(req.queryParams("id"));
            User user = new User(id);
            try {
                new Sql2oUserDao(getSql2o()).delete(user);
                res.status(204);
            } catch (DaoException ex) {
                res.status(404);
            }
            res.type("application/json");
            return new Gson().toJson(user.toString());
        });

        //availabilities route; lists all availabilities
        get("/availabilities", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            Sql2oAvailabilityDao sql2oAvailabilityDao = new Sql2oAvailabilityDao(getSql2o());
            String results = new Gson().toJson(sql2oAvailabilityDao.listAll());
            res.type("application/json");
            res.status(200);
            return results;
        });

        //delavailability route; deletes availabilities
        post("/delavailability", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            int calendarId = Integer.parseInt(req.queryParams("calendarId"));
            int date = Integer.parseInt(req.queryParams("date"));
            System.out.println(date);
            int qHour = Integer.parseInt(req.queryParams("qHour"));
            Availability a = new Availability(calendarId, date, qHour);
            try {
                new Sql2oAvailabilityDao(getSql2o()).delete(a);
                res.status(204);
            } catch (DaoException ex) {
                res.status(404);
            }
            res.type("application/json");
            return new Gson().toJson(a.toString());
        });

        //addavailability route; inserts a new availability
        post("/addavailability", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            int calendarId = Integer.parseInt(req.queryParams("calendarId"));
            int date = Integer.parseInt(req.queryParams("date"));
            System.out.println(date);
            int qHour = Integer.parseInt(req.queryParams("qHour"));
            Availability a = new Availability(calendarId, date, qHour);
            new Sql2oAvailabilityDao(getSql2o()).add(a);
            res.status(201);
            res.type("application/json");
            return new Gson().toJson(a.toString());
        });

        //updateavailability route; updates availability
        post("/api/updateavailability", (req, res) -> {
            Sql2o sql2o = getSql2o();
            int calendarId = Integer.parseInt(req.queryParams("calendarId"));
            int date = Integer.parseInt(req.queryParams("date"));
            System.out.println(date);
            int qHour = Integer.parseInt(req.queryParams("qHour"));
            int availstate = Integer.parseInt(req.queryParams("state"));
            Availability avail = new Availability(calendarId, date, qHour);
            boolean curAvail = new Sql2oAvailabilityDao(sql2o).updatecheck(avail);
            if (availstate == 1 && !curAvail) {
                new Sql2oAvailabilityDao(sql2o).add(avail);
                res.status(201);
                }
            else if (availstate == 0 && curAvail) {
                new Sql2oAvailabilityDao(sql2o).delete(avail);
                res.status(204);
            }


            res.type("application/json");
            return new Gson().toJson(avail.toString());
        });

        //editprofile route; edit profile fields
        post("/api/editprofile", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            String username = req.cookie("username");
            String email = req.queryParams("email");
            String affil = req.queryParams("affil");
            String title = req.queryParams("title");
            String description = req.queryParams("description");
            User u = new Sql2oUserDao(getSql2o()).getUserFromName(username);
            if (!email.equals("")) {
                new Sql2oUserDao(getSql2o()).setemail(u, email);
            }
            if (!affil.equals("")) {
                new Sql2oUserDao(getSql2o()).setaffil(u, affil);
            }
            if (!title.equals("")) {
                new Sql2oUserDao(getSql2o()).settitle(u, title);
            }
            if (!description.equals("")) {
                new Sql2oUserDao(getSql2o()).setdescription(u, description);
            }
            res.status(201);
            res.type("application/json");
            return new Gson().toJson(u.toString());
        });

        //getprofile route; get profile fields
        post("/api/getprofile", (req, res) -> {
            if (req.cookie("username") == null)
                res.redirect("/");
            String username = req.cookie("username");
            User u = new Sql2oUserDao(getSql2o()).getUserFromName(username);
            res.status(201);
            res.type("application/json");
            return new Gson().toJson(u);
        });


        makeStaticRoutes(
                Arrays.asList(
                          "/login"
                        , "/view-event", "/list-public-events"
                ),
                Arrays.asList(
                        new StaticRoutes("/", "index"),
                        new StaticRoutes("", "index")
                ),
                null
        );

        makeStaticRoutes(
                Arrays.asList(
                          "/profile", "/change-password", "/change-profile"
                        , "/create-calendar", "/list-calendar"
                        , "/create-event", "/list-events", "/dashboard"
                ),
                "/login"
        );

    }

    private static class StaticRoutes {
        public String routeName;
        public String fileName;

        public StaticRoutes(String routeName) {
            this.routeName = routeName;
            this.fileName = routeName.substring(1);
        }

        public StaticRoutes(String routeName, String fileName) {
            this.routeName = routeName;
            this.fileName = fileName;
        }
    }

    public static void makeStaticRoutes(List<String> simpleRoutes, String redirectLocation) {
        makeStaticRoutes(simpleRoutes, new ArrayList<>(), redirectLocation);
    }

    public static void makeStaticRoutes(List<String> simpleRoutes, List<StaticRoutes> otherRoutes, String redirectLocation) {
        Stream<StaticRoutes> staticRoutesStream =  Stream.concat(simpleRoutes.parallelStream().map(StaticRoutes::new).sequential(), otherRoutes.stream());
        if(redirectLocation == null) {
            staticRoutesStream.forEach(route -> get(route.routeName, (req, res) -> {
                res.status(200);
                res.type("text/html");
                return IOUtils.toString(Spark.class.getResourceAsStream("/public/static/html/" + route.fileName + ".html"));
            }));
        }
        else {
            staticRoutesStream.forEach(route -> get(route.routeName, (req, res) -> {
                res.status(200);
                res.type("text/html");
                if (req.cookie("username") == null) {
                    res.redirect(redirectLocation);
                    return "";
                }

                return IOUtils.toString(Spark.class.getResourceAsStream("/public/static/html/" + route.fileName + ".html"));
            }));
        }
    }
}
