package com.smartverse.churchlitebackend.controller.appointments;

import com.smartverse.churchlitebackend.service.appointments.AppointmentsBusinessService;

import com.smartverse.churchlitebackend_gen.endpoints.CreateRecurringAppointments;
import com.smartverse.churchlitebackend_gen.endpoints.CreateRecurringAppointmentsInput;
import com.smartverse.churchlitebackend_gen.endpoints.CreateRecurringAppointmentsOutput;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class AppointmentsCustomHandler implements CreateRecurringAppointments {

    private final AppointmentsBusinessService appointmentsService;

    public AppointmentsCustomHandler(AppointmentsBusinessService appointmentsService) {
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
