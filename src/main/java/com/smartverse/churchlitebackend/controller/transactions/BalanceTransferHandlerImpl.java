package com.smartverse.churchlitebackend.controller.transactions;

import com.smartverse.churchlitebackend.service.transactions.BalanceTransferService;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceTransferHandlerImpl implements CreateBalanceTransfer, GetBalanceTransfers, ReverseBalanceTransfer {
    private final BalanceTransferService service;

    public BalanceTransferHandlerImpl(BalanceTransferService service) { this.service = service; }

    @Override
    public ResponseEntity<CreateBalanceTransferOutput> createBalanceTransfer(CreateBalanceTransferInput input) {
        var output = new CreateBalanceTransferOutput();
        output.transfer = service.create(input.sourceId, input.destinationId, input.value, input.dateTransaction,
                input.description, input.observation);
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<GetBalanceTransfersOutput> getBalanceTransfers() {
        var output = new GetBalanceTransfersOutput();
        output.accounts = service.accounts();
        output.transfers = service.history();
        return ResponseEntity.ok(output);
    }

    @Override
    public ResponseEntity<ReverseBalanceTransferOutput> reverseBalanceTransfer(ReverseBalanceTransferInput input) {
        service.reverse(input.transferId);
        var output = new ReverseBalanceTransferOutput();
        output.reversed = true;
        return ResponseEntity.ok(output);
    }
}
