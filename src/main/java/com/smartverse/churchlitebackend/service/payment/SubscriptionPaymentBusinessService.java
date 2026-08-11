package com.smartverse.churchlitebackend.service.payment;

import com.potatotech.authorization.exception.ServiceException;
import com.potatotech.authorization.tenant.TenantContext;
import com.smartverse.churchlitebackend.config.database.TenantSchemaInterceptor;
import com.smartverse.churchlitebackend.config.migration.DBMigration;
import com.smartverse.churchlitebackend.service.subscription.SubscriptionCache;
import com.smartverse.churchlitebackend_gen.entities.*;
import com.smartverse.churchlitebackend_gen.repositories.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SubscriptionPaymentBusinessService {
 private final SubscriptionPaymentRepository payments; private final PaymentEventInboxRepository inbox;
 private final SubscriptionPlanRepository plans; private final TenantSubscriptionRepository subscriptions;
 private final SubscriptionCache cache; private final EntityManager em; private final DBMigration migration;
 private final TenantSchemaInterceptor schema; private final RestClient client;
 public SubscriptionPaymentBusinessService(SubscriptionPaymentRepository payments,PaymentEventInboxRepository inbox,SubscriptionPlanRepository plans,TenantSubscriptionRepository subscriptions,SubscriptionCache cache,EntityManager em,DBMigration migration,TenantSchemaInterceptor schema,RestClient.Builder builder,@Value("${payment.service.base-url:${PAYMENT_SERVICE_BASE_URL:https://app.smartverse.com.br/api/payment-service}}") String url){this.payments=payments;this.inbox=inbox;this.plans=plans;this.subscriptions=subscriptions;this.cache=cache;this.em=em;this.migration=migration;this.schema=schema;this.client=builder.baseUrl(url).build();}
 @Transactional public Checkout checkout(String planCode,String cycle,String authorization){
  String tenant=requireTenant(); Cycle c=Cycle.parse(cycle); if(authorization==null||authorization.isBlank()) throw error(HttpStatus.UNAUTHORIZED,"payment_authorization_required");
  try{switchTo("admin"); var plan=plans.findAll().stream().filter(p->p.isActive()&&p.getCode().equalsIgnoreCase(planCode)).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"subscription_plan_not_found")); if("FREE".equalsIgnoreCase(plan.getCode()))throw error(HttpStatus.BAD_REQUEST,"subscription_free_checkout_unavailable");
   int cents=BigDecimal.valueOf(plan.getPriceMonthly()).multiply(BigDecimal.valueOf(c.months)).multiply(BigDecimal.ONE.subtract(c.discount.movePointLeft(2))).setScale(2,RoundingMode.HALF_UP).movePointRight(2).intValueExact();
   var payment=payments.findAll().stream().filter(p->tenant.equalsIgnoreCase(p.getTenant())&&plan.getCode().equalsIgnoreCase(p.getPlanCode())&&c.name().equals(p.getBillingCycle())&&"PENDING".equals(p.getStatus())).max(Comparator.comparing(SubscriptionPaymentEntity::getCreatedAt)).orElse(null);
   boolean localReuse=payment!=null; if(payment==null){payment=new SubscriptionPaymentEntity();payment.setTenant(tenant);payment.setPlanCode(plan.getCode());payment.setBillingCycle(c.name());payment.setMonths(c.months);payment.setAmountCents(cents);payment.setStatus("PENDING");payment.setCreatedAt(LocalDateTime.now());payment=payments.save(payment);payments.flush();}
   var response=client.post().uri("/paymentLink").header(HttpHeaders.AUTHORIZATION,authorization).contentType(MediaType.APPLICATION_JSON).body(Map.of("service","CHURCH_LITE","value",payment.getAmountCents(),"client_id",payment.getId().toString())).retrieve().body(PaymentLinkResponse.class);
   if(response==null||response.url==null||response.order_nsu==null)throw error(HttpStatus.BAD_GATEWAY,"payment_service_invalid_response"); payment.setOrderNsu(response.order_nsu);payments.save(payment);return new Checkout(response.url,response.order_nsu,response.status,response.reused||localReuse);
  }catch(ServiceException e){throw e;}catch(Exception e){throw error(HttpStatus.BAD_GATEWAY,"payment_service_unavailable");}finally{switchTo(tenant);}
 }
 @Transactional public void confirm(Event e){validate(e);switchTo("admin"); try{var old=inbox.findAll().stream().filter(i->e.paymentId.equals(i.getPaymentId())).findFirst().orElse(null);if(old!=null&&"PROCESSED".equals(old.getStatus()))return;var row=old==null?new PaymentEventInboxEntity():old;row.setPaymentId(e.paymentId);row.setClientId(e.clientId);row.setStatus("RECEIVED");row.setReceivedAt(LocalDateTime.now());row=inbox.save(row);try{var p=payments.findById(e.clientId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"payment_not_found"));if(!Objects.equals(p.getOrderNsu(),e.orderNsu)||p.getAmountCents()!=e.amount||e.paidAmount<p.getAmountCents())throw error(HttpStatus.CONFLICT,"payment_confirmation_mismatch");p.setStatus("PAID");p.setTransactionNsu(e.transactionNsu);p.setPaidAt(e.paidAt);payments.save(p);activate(p);row.setStatus("PROCESSED");row.setProcessedAt(LocalDateTime.now());row.setFailureReason(null);inbox.save(row);}catch(Exception x){row.setStatus("FAILED");row.setFailureReason(x.getMessage()==null?"payment_processing_failed":x.getMessage().substring(0,Math.min(1000,x.getMessage().length())));inbox.save(row);throw x;}}finally{TenantContext.setCurrentTenant("admin");}}
 private void activate(SubscriptionPaymentEntity p){switchTo(p.getTenant());var plan=plans.findAll().stream().filter(x->x.getCode().equalsIgnoreCase(p.getPlanCode())).findFirst().orElseThrow();var s=subscriptions.findAll().stream().filter(x->p.getTenant().equalsIgnoreCase(x.getTenant())).findFirst().orElseGet(TenantSubscriptionEntity::new);var now=p.getPaidAt();var base=s.getCurrentPeriodEndsAt()!=null&&s.getCurrentPeriodEndsAt().isAfter(now)?s.getCurrentPeriodEndsAt():now;s.setTenant(p.getTenant());s.setPlan(plan);s.setStatus("ACTIVE");s.setStartedAt(now);s.setCurrentPeriodEndsAt(base.plusMonths(p.getMonths()));s.setPriceMonthly(plan.getPriceMonthly());subscriptions.save(s);cache.evict(p.getTenant());switchTo("admin");}
 private void validate(Event e){if(e==null||e.paymentId==null||e.clientId==null||!"CHURCH_LITE".equals(e.service)||e.amount<=0||e.paidAmount<=0||e.paidAt==null)throw error(HttpStatus.BAD_REQUEST,"payment_event_invalid");}
 private String requireTenant(){var t=TenantContext.getCurrentTenant();if(t==null||t.isBlank()||"admin".equalsIgnoreCase(t))throw error(HttpStatus.FORBIDDEN,"subscription_tenant_not_identified");return t;}
 private void switchTo(String t){em.flush();em.clear();migration.loadMigrateTenants(t);TenantContext.setCurrentTenant(t);schema.switchSchema();}
 private ServiceException error(HttpStatus s,String m){return new ServiceException(s,m);}
 enum Cycle{MONTHLY(1,"0"),QUARTERLY(3,"10"),SEMIANNUAL(6,"15");final int months;final BigDecimal discount;Cycle(int m,String d){months=m;discount=new BigDecimal(d);}static Cycle parse(String s){try{return valueOf(s);}catch(Exception e){throw new ServiceException(HttpStatus.BAD_REQUEST,"billing_cycle_invalid");}}}
 public record Checkout(String url,String orderNsu,String status,boolean reused){}
 public static class PaymentLinkResponse{public String url;public String order_nsu;public String status;public boolean reused;}
 public static class Event{public UUID paymentId;public UUID clientId;public String service;public String orderNsu;public String transactionNsu;public int amount;public int paidAmount;public LocalDateTime paidAt;}
}
