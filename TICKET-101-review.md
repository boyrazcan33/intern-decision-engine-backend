# TICKET-101 Review – Backend and Frontend

## Backend

### What Was Done Well

1.	The codebase is structured clearly, separating controller and service logic.
2.	Exception handling is clean and uses custom exceptions for personal code, loan amount, and loan period.
3.	Response structure and status codes are used properly (e.g. 400, 404, 500).
4.	The use of constants in a separate config class (`DecisionEngineConstants`) helps readability and maintainability.
5.	The business logic is easy to follow after reading through the code.
6.	While reviewing the exceptions, I decided it was better to keep them separate instead of combining them into one. This makes it easier to debug and handle specific errors, and helps with logging and testing.


### What Needed Fixing or Improvement

1.	The most critical issue was that the scoring formula was missing. Instead of using the formula described in the task, the code was just multiplying `creditModifier * loanPeriod`. I replaced this with the correct formula: ((creditModifier / loanAmount) * loanPeriod) / 10 ≥ 0.1

2.	The maximum loan period was set to 60 months in the code, but the task requirement was 48. I updated this in the constants.
3.	`DecisionResponse` was originally injected using `@Autowired`, which can cause issues in multi-threaded environments. I changed this to a local object in the controller.
4.	The field `creditModifier` was unnecessarily declared as a class field when it’s only used in one method. I moved it to a local variable.
5.	`DecisionRequest` was using Lombok’s `@AllArgsConstructor`, but didn’t include a no-args constructor. Since Jackson requires a no-args constructor to deserialize incoming JSON, I added `@NoArgsConstructor` to prevent errors during request mapping.
6.	The method getCreditModifier() returns 0 when the personal code segment indicates debt. Replacing this with a named constant like NO_CREDIT_MODIFIER improves readability and makes the code more self-explanatory.
---

## Frontend

### What Was Done Well

1.	The project is well-structured. UI logic (`LoanForm`), API logic (`ApiService`), and models are properly separated.
2.	The form works and successfully sends data to the backend and displays the result or error.
3.	Code uses Flutter best practices like `TextEditingController` and state updates with `setState`.

### What Could Be Improved

1.	Originally, the form triggered the API call automatically when the personal ID was entered, without any visible submit button. This made the experience unclear, since there was no way to know when the request was being made or completed. To improve this, I added a visible Submit button and a loading spinner to give better feedback and control over the process.
2.	The personal ID field has no validation. Even a basic "field required" check would be useful to prevent accidental empty submissions.
3.	The loan period slider allows values up to 60 months, but the backend rejects anything over 48. The slider should be updated to cap at 48 to match the business rules.

---

## Conclusion

The backend originally missed the core scoring logic required by the task. I fixed this by implementing the correct formula and updating the loan period limit to 48 months. I also made minor improvements like using local variables and removing risky shared state in the controller.

The frontend works well overall and talks to the backend as expected. A few small improvements like validation, feedback on button clicks, and matching slider limits would make the user experience smoother.

Both sides of the project are functional and easy to understand. After these adjustments, the app behaves correctly according to the task requirements.

