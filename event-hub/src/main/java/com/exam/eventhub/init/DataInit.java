package com.exam.eventhub.init;

import com.exam.eventhub.category.service.CategoryService;
import com.exam.eventhub.event.service.EventService;
import com.exam.eventhub.user.service.UserService;
import com.exam.eventhub.venue.service.VenueService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DataInit implements CommandLineRunner {

    private final CategoryService categoryService;
    private final VenueService venueService;
    private final UserService userService;
    private final EventService eventService;

    @Override
    public void run(String... args)  {
        this.categoryService.initData();
        this.venueService.initData();
        this.userService.initData();
        this.eventService.initData();
    }

}
