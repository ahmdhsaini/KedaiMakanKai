# Customer Creation Debug Guide

## Problem
Customer record is not being saved to CUSTOMER table after user registration.

## Potential Issues

### 1. Authentication Required
- **Issue**: POST /CUSTOMER might require `api-key` header
- **During Registration**: User has no token yet, so api-key header is empty
- **Solution**: Backend should allow customer creation without auth OR auto-create customer during registration

### 2. Backend API Not Processing
- **Check**: Does your backend Java API actually process POST /CUSTOMER?
- **Check**: Does it save to database or just return success without saving?

### 3. Database Constraints
- **Check**: Does CUSTOMER table have NOT NULL constraints on other fields?
- **Check**: Is UserID a foreign key that requires the users record to exist first?
- **Check**: Are there any triggers or validation that might be blocking the insert?

### 4. Request Format
- **Current**: Sending form-urlencoded with UserID, CustName, CustPhone
- **Check**: Does backend expect JSON instead?
- **Check**: Are field names correct (UserID vs userId)?

## How to Debug

1. **Check Logcat** (filter by "RegisterActivity"):
   - Look for "Sending POST /CUSTOMER with: ..."
   - Look for "Customer creation response - HTTP: ..."
   - Look for error messages

2. **Check Backend Logs**:
   - Is the POST /CUSTOMER request reaching your backend?
   - What HTTP status code is returned?
   - Are there any error messages in backend logs?

3. **Check Database**:
   - After registration, manually check if customer record exists
   - Check if there are any database errors

## Recommended Solutions

### Option 1: Backend Auto-Create (BEST)
Modify your backend registration endpoint to automatically create customer record:
```java
// In your registration controller
@POST("users/register")
public SimpleResponse register(@RequestParam String email, 
                               @RequestParam String username, 
                               @RequestParam String password,
                               @RequestParam String custName,
                               @RequestParam String custPhone) {
    // 1. Create user
    User user = userService.createUser(email, username, password);
    
    // 2. Create customer in same transaction
    Customer customer = customerService.createCustomer(user.getId(), custName, custPhone);
    
    return new SimpleResponse(true, "Registration successful", user.getId());
}
```

### Option 2: Allow Unauthenticated Customer Creation
Modify your backend to allow POST /CUSTOMER without authentication during registration.

### Option 3: Return Token After Registration
Return a temporary token after registration that can be used to create customer record.
