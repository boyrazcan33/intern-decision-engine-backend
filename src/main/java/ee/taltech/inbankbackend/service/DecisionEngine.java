package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.AgeLimitConstants;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidAgeException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

// Comments are updated for better understanding of the code logic.
@Service
public class DecisionEngine {

    private final EstonianPersonalCodeValidator validator = new EstonianPersonalCodeValidator();

    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException, InvalidAgeException {

        // First validate all inputs including age, loan amount, loan period, and personal code
        verifyInputs(personalCode, loanAmount, loanPeriod);

        // Determine the credit modifier based on the segment encoded in the personal code
        int creditModifier = getCreditModifier(personalCode);

        // If the applicant has debt, they are not eligible for a loan
        if (creditModifier == DecisionEngineConstants.NO_CREDIT_MODIFIER) {
            throw new NoValidLoanException("No valid loan found!");
        }

        // Try to find the best possible loan offer within the allowed limits
        int outputLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT;
        int approvedAmount = 0;
        int approvedPeriod = loanPeriod;

        // Check increasing periods to find a combination that qualifies
        while (approvedPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            for (int amount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
                 amount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT; amount -= 100) {

                // Apply the credit score formula
                double creditScore = ((double) creditModifier / amount) * approvedPeriod / 10;

                // If the score is acceptable, save this result
                if (creditScore >= 0.1) {
                    approvedAmount = amount;
                    break;
                }
            }

            if (approvedAmount >= DecisionEngineConstants.MINIMUM_LOAN_AMOUNT) {
                break;
            }

            approvedPeriod++;
        }

        // If no valid amount is found within allowed limits, reject the loan
        if (approvedAmount < DecisionEngineConstants.MINIMUM_LOAN_AMOUNT
                || approvedPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            throw new NoValidLoanException("No valid loan found!");
        }

        // Return the approved loan amount and period
        return new Decision(approvedAmount, approvedPeriod, null);
    }

    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, InvalidAgeException {

        // Validate that the personal code has the correct format
        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }

        // Check if the applicant is old enough and not beyond the allowed age
        validateAge(personalCode);

        // Check if the requested loan amount is within allowed range
        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }

        // Check if the requested loan period is within allowed range
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }
    }

    /**
     * Validates the applicant's age based on the birth date encoded in the personal code.
     * Also determines which country the applicant belongs to and applies the expected life limit.
     */
    private void validateAge(String personalCode) throws InvalidAgeException {
        // Parse birth day, month, and year from the personal ID code
        int day = Integer.parseInt(personalCode.substring(5, 7));
        int month = Integer.parseInt(personalCode.substring(3, 5));
        String centuryIndicator = personalCode.substring(0, 1);
        String yearFragment = personalCode.substring(1, 3);

        int year;
        if (centuryIndicator.equals("3") || centuryIndicator.equals("4")) {
            year = Integer.parseInt("19" + yearFragment);
        } else {
            year = Integer.parseInt("20" + yearFragment);
        }

        // Calculate the applicant's age
        LocalDate birthDate = LocalDate.of(year, month, day);
        LocalDate today = LocalDate.now();
        int age = today.getYear() - birthDate.getYear();
        if (today.getDayOfYear() < birthDate.getDayOfYear()) {
            age--;
        }

        // Reject if under minimum allowed age
        if (age < AgeLimitConstants.MINIMUM_AGE) {
            throw new InvalidAgeException("Applicant is under the minimum age.");
        }

        // Determine the country based on the personal code segment
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));
        int expectedLifetime;

        if (segment < 3333) {
            expectedLifetime = AgeLimitConstants.LIFETIME_ESTONIA;
        } else if (segment < 6666) {
            expectedLifetime = AgeLimitConstants.LIFETIME_LATVIA;
        } else {
            expectedLifetime = AgeLimitConstants.LIFETIME_LITHUANIA;
        }

        // Calculate the max allowed age by subtracting the maximum loan period (in years)
        int maxEligibleAge = expectedLifetime - (DecisionEngineConstants.MAXIMUM_LOAN_PERIOD / 12);

        // Reject if older than max allowed age
        if (age > maxEligibleAge) {
            throw new InvalidAgeException("Applicant exceeds maximum eligible age.");
        }
    }

    /**
     * Returns a credit modifier based on the last 4 digits of the personal code.
     * Lower numbers indicate debt; higher numbers belong to different scoring segments.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return DecisionEngineConstants.NO_CREDIT_MODIFIER;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }
}
