package ee.taltech.inbankbackend.endpoint;

import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import ee.taltech.inbankbackend.service.Decision;
import ee.taltech.inbankbackend.service.DecisionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ee.taltech.inbankbackend.exceptions.InvalidAgeException;
import ee.taltech.inbankbackend.config.AgeLimitConstants;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;


@RestController
@RequestMapping("/loan")
@CrossOrigin
public class DecisionEngineController {

    private final DecisionEngine decisionEngine;

    @Autowired
    DecisionEngineController(DecisionEngine decisionEngine) {
        this.decisionEngine = decisionEngine;
    }

    @PostMapping("/decision")
    public ResponseEntity<DecisionResponse> requestDecision(@RequestBody DecisionRequest request) {
        DecisionResponse response = new DecisionResponse();

        try {
            Decision decision = decisionEngine.calculateApprovedLoan(
                    request.getPersonalCode(),
                    request.getLoanAmount(),
                    request.getLoanPeriod());

            response.setLoanAmount(decision.getLoanAmount());
            response.setLoanPeriod(decision.getLoanPeriod());
            response.setErrorMessage(decision.getErrorMessage());

            return ResponseEntity.ok(response);

        } catch (InvalidPersonalCodeException | InvalidLoanAmountException | InvalidLoanPeriodException e) {
            response.setLoanAmount(null);
            response.setLoanPeriod(null);
            response.setErrorMessage(e.getMessage());

            return ResponseEntity.badRequest().body(response);


        } catch (InvalidAgeException e) {
            response.setLoanAmount(null);
            response.setLoanPeriod(null);
            response.setErrorMessage(e.getMessage());

            return ResponseEntity.badRequest().body(response);
        } catch (NoValidLoanException e) {
            response.setLoanAmount(null);
            response.setLoanPeriod(null);
            response.setErrorMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception e) {
            response.setLoanAmount(null);
            response.setLoanPeriod(null);
            response.setErrorMessage("An unexpected error occurred");

            return ResponseEntity.internalServerError().body(response);
        }
    }
}
