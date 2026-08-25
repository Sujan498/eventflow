package com.eventflow.eventflow.config;

import com.eventflow.eventflow.entity.*;
import com.eventflow.eventflow.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a demo catalogue on first boot so the hosted instance has browsable data.
 * Idempotent: skips entirely if any venue already exists.
 * Inserts through repositories rather than the admin services so seeding never
 * depends on request-level validation or an authenticated principal.
 * All venues and events are fictional.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final int ROWS = 6;
    private static final int SEATS_PER_ROW = 10;

    private final VenueRepository venueRepository;
    private final HallRepository hallRepository;
    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(VenueRepository venueRepository, HallRepository hallRepository,
                      SeatRepository seatRepository, EventRepository eventRepository,
                      UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.venueRepository = venueRepository;
        this.hallRepository = hallRepository;
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (venueRepository.count() > 0) {
            log.info("DataSeeder: catalogue already present, skipping.");
            return;
        }
        log.info("DataSeeder: empty database detected, seeding demo data.");

        User admin = user("Demo", "Admin", "admin@eventflow.app", "+919000000001", "Admin@1234", Role.ADMIN);
        user("Demo", "User", "demo@eventflow.app", "+919000000002", "Demo@1234", Role.USER);

        List<Hall> halls = new ArrayList<>();
        halls.addAll(venueWithHalls("Lakeside Convention Hall", "Outer Ring Road, Bellandur",
                "Bengaluru", "Karnataka", "India", 12.9257, 77.6740, 2));
        halls.addAll(venueWithHalls("Cubbon Arts Theatre", "Kasturba Road, near Cubbon Park",
                "Bengaluru", "Karnataka", "India", 12.9763, 77.5929, 2));

        Instant now = Instant.now();
        String[][] events = {
                {"Bengaluru Indie Music Night", "Six local bands, one stage, no covers.", "3", "2", "899"},
                {"Stand-up: Late Shift", "An evening of comedy about working in tech.", "5", "2", "499"},
                {"Distributed Systems 101", "A hands-on talk on consensus, queues, and why your cache is lying.", "8", "3", "0"},
                {"Veena & Tabla: Classical Evening", "A two-hour Carnatic and Hindustani jugalbandi.", "12", "3", "1200"},
                {"Monsoon Street (Stage Adaptation)", "The tea-stall drama, live on stage in Kannada.", "16", "2", "650"},
                {"Startup Pitch Day", "Twelve early-stage founders, five minutes each, one panel.", "21", "4", "0"}
        };
        for (int i = 0; i < events.length; i++) {
            String[] e = events[i];
            Instant start = now.plus(Long.parseLong(e[2]), ChronoUnit.DAYS)
                    .truncatedTo(ChronoUnit.HOURS).plus(18, ChronoUnit.HOURS);
            Event ev = new Event();
            ev.setId(UUID.randomUUID());
            ev.setTitle(e[0]);
            ev.setDescription(e[1]);
            ev.setBannerUrl("https://placehold.co/1200x400/1a1a2e/ffffff?text=" + e[0].replace(" ", "+"));
            ev.setStartTime(start);
            ev.setEndTime(start.plus(Long.parseLong(e[3]), ChronoUnit.HOURS));
            ev.setBasePrice(new BigDecimal(e[4]));
            ev.setStatus(EventStatus.PUBLISHED);
            ev.setOrganizer(admin);
            ev.setHall(halls.get(i % halls.size()));
            ev.setCreatedAt(now);
            ev.setUpdatedAt(now);
            eventRepository.save(ev);
        }

        log.info("DataSeeder: seeded 2 venues, {} halls, {} events; logins admin@eventflow.app / demo@eventflow.app",
                halls.size(), events.length);
    }

    private User user(String first, String last, String email, String phone, String rawPassword, Role role) {
        Instant now = Instant.now();
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPhoneNumber(phone);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setDateOfBirth(LocalDate.of(1998, 1, 1));
        u.setRole(role);
        u.setEnabled(true);
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return userRepository.save(u);
    }

    private List<Hall> venueWithHalls(String name, String address, String city, String state, String country,
                                      double lat, double lon, int hallCount) {
        Instant now = Instant.now();
        Venue v = new Venue();
        v.setId(UUID.randomUUID());
        v.setName(name);
        v.setAddress(address);
        v.setCity(city);
        v.setState(state);
        v.setCountry(country);
        v.setLatitude(lat);
        v.setLongitude(lon);
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        v = venueRepository.save(v);

        List<Hall> halls = new ArrayList<>();
        for (int h = 1; h <= hallCount; h++) {
            Hall hall = new Hall();
            hall.setId(UUID.randomUUID());
            hall.setHallNumber(h);
            hall.setCapacity(ROWS * SEATS_PER_ROW);
            hall.setVenue(v);
            hall.setCreatedAt(now);
            hall.setUpdatedAt(now);
            hall = hallRepository.save(hall);

            List<Seat> seats = new ArrayList<>();
            for (int r = 0; r < ROWS; r++) {
                for (int n = 1; n <= SEATS_PER_ROW; n++) {
                    Seat s = new Seat();
                    s.setId(UUID.randomUUID());
                    s.setRowLabel((char) ('A' + r));
                    s.setSeatNumber(n);
                    s.setSeatType(SeatType.EXECUTIVE);
                    s.setHall(hall);
                    s.setCreatedAt(now);
                    s.setUpdatedAt(now);
                    seats.add(s);
                }
            }
            seatRepository.saveAll(seats);
            halls.add(hall);
        }
        return halls;
    }
}
