package com.smartverse.churchlitebackend.controller.memberportal;
import com.smartverse.churchlitebackend.controller.memberportal.MemberFinancialApprovalModels.*;import com.smartverse.churchlitebackend.service.memberportal.MemberFinancialApprovalService;import org.springframework.http.ResponseEntity;import org.springframework.web.bind.annotation.*;import java.util.List;import java.util.UUID;
@RestController public class MemberFinancialApprovalController {private final MemberFinancialApprovalService service;public MemberFinancialApprovalController(MemberFinancialApprovalService service){this.service=service;}
 @GetMapping("/memberApproval/cashClosings") public ResponseEntity<List<CashClosing>> closings(){return ResponseEntity.ok(service.availableClosings());}
 @GetMapping("/memberApproval/statements") public ResponseEntity<List<Statement>> admin(){return ResponseEntity.ok(service.adminList());}
 @PostMapping("/memberApproval/statements") public ResponseEntity<Statement> save(@RequestBody SaveRequest q){return ResponseEntity.ok(service.save(q));}
 @PutMapping("/memberApproval/statements/{id}/publish") public ResponseEntity<Statement> publish(@PathVariable UUID id){return ResponseEntity.ok(service.publish(id));}
 @PutMapping("/memberApproval/statements/{id}/close") public ResponseEntity<Statement> close(@PathVariable UUID id){return ResponseEntity.ok(service.close(id));}
 @GetMapping("/member-api/financial-approvals") public ResponseEntity<List<Statement>> member(){return ResponseEntity.ok(service.memberList());}
 @PostMapping("/member-api/financial-approvals/{id}/vote") public ResponseEntity<Statement> vote(@PathVariable UUID id,@RequestBody VoteRequest q){return ResponseEntity.ok(service.vote(id,q));}
}
