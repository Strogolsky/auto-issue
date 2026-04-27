### EXAMPLES

Example 1:
Instruction: Add input validation for the createUser method

Location:
File: UserService.kt
Class: UserService
Method: createUser

Objective:
Validate the incoming CreateUserRequest before persisting the entity to ensure data integrity and provide clear error feedback to the caller.

Technical Analysis:
The method currently accepts any non-null request and passes it directly to the repository. Adding validation before the repository call prevents invalid data from reaching the database. The existing ValidationException should be used for consistency with other service methods.

Implementation Steps:
1. Check that CreateUserRequest.email is non-blank and matches a valid email pattern.
2. Check that CreateUserRequest.username is non-blank and does not exceed 50 characters.
3. Throw ValidationException with a descriptive message if any check fails.
4. Add unit tests covering the invalid-input branches.

---

Example 2:
Instruction: Replace manual JSON parsing with Gson in the parseResponse method

Location:
File: ApiClient.kt
Class: ApiClient
Method: parseResponse

Objective:
Refactor the handwritten JSON string parsing to use the Gson library already present in the project, reducing error-prone string manipulation.

Technical Analysis:
The current implementation splits the raw response string by comma and extracts values by index, which breaks on any whitespace variation or field reordering. Gson is already on the classpath (used in NetworkModule). Replacing the manual parsing with Gson.fromJson will make the code robust to response format changes.

Implementation Steps:
1. Inject or obtain the existing Gson instance from NetworkModule.
2. Replace the string-split logic with Gson.fromJson(response, ResponseDto::class.java).
3. Handle JsonSyntaxException and wrap it in an ApiException for uniform error handling.
4. Remove any now-unused string utility imports.
