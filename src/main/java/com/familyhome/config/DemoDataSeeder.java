package com.familyhome.config;

import com.familyhome.event.Event;
import com.familyhome.event.EventRepository;
import com.familyhome.member.AppUser;
import com.familyhome.member.AppUserRepository;
import com.familyhome.shopping.ShoppingItem;
import com.familyhome.shopping.ShoppingItemRepository;
import com.familyhome.task.OneTimeTask;
import com.familyhome.task.RecurringTask;
import com.familyhome.task.TaskRepository;
import com.familyhome.vehicle.Vehicle;
import com.familyhome.vehicle.VehicleBooking;
import com.familyhome.vehicle.VehicleBookingRepository;
import com.familyhome.vehicle.VehicleRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@Profile("demo")
public class DemoDataSeeder {

    @Bean
    public CommandLineRunner seedDemoData(
            AppUserRepository userRepository,
            TaskRepository taskRepository,
            ShoppingItemRepository shoppingRepository,
            EventRepository eventRepository,
            VehicleRepository vehicleRepository,
            VehicleBookingRepository bookingRepository,
            PasswordEncoder passwordEncoder,
            Clock clock,
            @Value("${app.demo.username:demo}") String demoUsername,
            @Value("${app.demo.password:DemoPass123!}") String demoPassword) {
        return new DemoSeederRunner(
                userRepository,
                taskRepository,
                shoppingRepository,
                eventRepository,
                vehicleRepository,
                bookingRepository,
                passwordEncoder,
                clock,
                demoUsername,
                demoPassword);
    }

    static class DemoSeederRunner implements CommandLineRunner {

        private final AppUserRepository users;
        private final TaskRepository tasks;
        private final ShoppingItemRepository shopping;
        private final EventRepository events;
        private final VehicleRepository vehicles;
        private final VehicleBookingRepository bookings;
        private final PasswordEncoder passwords;
        private final Clock clock;
        private final String demoUsername;
        private final String demoPassword;

        DemoSeederRunner(
                AppUserRepository users,
                TaskRepository tasks,
                ShoppingItemRepository shopping,
                EventRepository events,
                VehicleRepository vehicles,
                VehicleBookingRepository bookings,
                PasswordEncoder passwords,
                Clock clock,
                String demoUsername,
                String demoPassword) {
            this.users = users;
            this.tasks = tasks;
            this.shopping = shopping;
            this.events = events;
            this.vehicles = vehicles;
            this.bookings = bookings;
            this.passwords = passwords;
            this.clock = clock;
            this.demoUsername = demoUsername;
            this.demoPassword = demoPassword;
        }

        @Override
        @Transactional
        public void run(String... args) {
            AppUser demo =
                    users.findByUsername(demoUsername)
                            .orElseGet(() -> {
                                AppUser u =
                                        new AppUser(demoUsername, passwords.encode(demoPassword));
                                u.changeDriver(true);
                                u.changeTaskManager(true);
                                return users.save(u);
                            });

            AppUser maya =
                    users.findByUsername("maya")
                            .orElseGet(() -> {
                                AppUser u = new AppUser("maya", passwords.encode(demoPassword));
                                u.changeDriver(true);
                                return users.save(u);
                            });

            AppUser alex =
                    users.findByUsername("alex")
                            .orElseGet(() ->
                                    users.save(
                                            new AppUser("alex", passwords.encode(demoPassword))));

            if (tasks.count() > 0
                    || shopping.count() > 0
                    || events.count() > 0
                    || vehicles.count() > 0) {
                return;
            }

            LocalDate today = LocalDate.now(clock);
            Instant now = Instant.now(clock);

            OneTimeTask groceryTask =
                    new OneTimeTask("קניות לשבת", "ירקות, חלות וחלב", demo, today.plusDays(1));
            OneTimeTask doneTask = new OneTimeTask("סידור מחסן", "מיון קופסאות", maya, today);
            doneTask.markAsCompleted(maya.getUsername(), now);

            RecurringTask trashSun =
                    new RecurringTask("הורדת זבל", "פח ירוק וכתום", demo, DayOfWeek.SUNDAY);
            RecurringTask trashWed =
                    new RecurringTask("הורדת זבל", "פח ירוק וכתום", demo, DayOfWeek.WEDNESDAY);
            RecurringTask laundry =
                    new RecurringTask("כביסה וקיפול", "מכונה בהירה", alex, DayOfWeek.TUESDAY);

            tasks.saveAll(List.of(groceryTask, doneTask, trashSun, trashWed, laundry));

            shopping.saveAll(
                    List.of(
                            new ShoppingItem("חלב 3%", 2, now),
                            new ShoppingItem("לחם מלא", 1, now.minusSeconds(60)),
                            new ShoppingItem("עגבניות שרי", 3, now.minusSeconds(120))));

            events.save(
                    new Event("ארוחת שישי משפחתית", today.plusDays(3), Set.of(demo, maya, alex)));

            Vehicle familyCar = vehicles.save(new Vehicle("רכב משפחתי"));
            vehicles.save(new Vehicle("רכב שני"));
            bookings.save(
                    new VehicleBooking(
                            familyCar, demo, today.plusDays(1), VehicleBooking.Slot.MORNING));
        }
    }
}
