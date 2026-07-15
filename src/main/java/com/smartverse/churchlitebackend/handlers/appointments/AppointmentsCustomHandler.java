package com.smartverse.churchlitebackend.handlers.appointments;

import com.smartverse.churchlitebackend.services.appointments.AppointmentsService;
import com.smartverse.churchlitebackend_gen.CreateRecurringAppointments;
import com.smartverse.churchlitebackend_gen.CreateRecurringAppointmentsInput;
import com.smartverse.churchlitebackend_gen.CreateRecurringAppointmentsOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AppointmentsCustomHandler implements CreateRecurringAppointments {

    private final AppointmentsService appointmentsService;

    public AppointmentsCustomHandler(AppointmentsService appointmentsService) {
        this.appointmentsService = appointmentsService;
    }

    @Override
    public ResponseEntity<CreateRecurringAppointmentsOutput> createRecurringAppointments(
            CreateRecurringAppointmentsInput input) {
        var output = new CreateRecurringAppointmentsOutput();
        output.appointments = appointmentsService.createRecurring(input.appointment);
        return ResponseEntity.ok(output);
    }
}
