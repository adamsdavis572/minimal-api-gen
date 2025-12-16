# Example: Exception Handler with useGlobalExceptionHandler=true

**File**: Program.cs (with useGlobalExceptionHandler=true, useProblemDetails=true)

```csharp
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Diagnostics;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddProblemDetails();

var app = builder.Build();

// Exception handler middleware
app.UseExceptionHandler(exceptionHandlerApp =>
{
    exceptionHandlerApp.Run(async context =>
    {
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        context.Response.ContentType = "application/problem+json";
        
        var exceptionHandlerPathFeature = context.Features.Get<IExceptionHandlerPathFeature>();
        var exception = exceptionHandlerPathFeature?.Error;
        
        var problemDetails = new ProblemDetails
        {
            Status = StatusCodes.Status500InternalServerError,
            Title = "An error occurred while processing your request",
            Detail = exception?.Message ?? "An unexpected error occurred"
        };
        
        await context.Response.WriteAsJsonAsync(problemDetails);
    });
});

app.MapAllEndpoints();

app.Run();
```

---

## Example Error Response

**Request**: `POST /pet` with handler that throws exception

**Response** (HTTP 500):
```json
{
  "type": "https://tools.ietf.org/html/rfc7231#section-6.6.1",
  "title": "An error occurred while processing your request",
  "status": 500,
  "detail": "Object reference not set to an instance of an object"
}
```

---

## Contrast: useGlobalExceptionHandler=false

**File**: Program.cs (with useGlobalExceptionHandler=false)

```csharp
using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddProblemDetails();

var app = builder.Build();

// No exception handler middleware
// Unhandled exceptions return default ASP.NET Core error page

app.MapAllEndpoints();

app.Run();
```

**Behavior**: Unhandled exceptions result in default ASP.NET Core error response (not ProblemDetails format).

---

## Contrast: useGlobalExceptionHandler=true, useProblemDetails=false

**File**: Program.cs

```csharp
app.UseExceptionHandler(exceptionHandlerApp =>
{
    exceptionHandlerApp.Run(async context =>
    {
        context.Response.StatusCode = StatusCodes.Status500InternalServerError;
        context.Response.ContentType = "application/json";
        
        var exceptionHandlerPathFeature = context.Features.Get<IExceptionHandlerPathFeature>();
        var exception = exceptionHandlerPathFeature?.Error;
        
        var errorResponse = new
        {
            error = "Internal Server Error",
            message = exception?.Message ?? "An unexpected error occurred"
        };
        
        await context.Response.WriteAsJsonAsync(errorResponse);
    });
});
```

**Response** (HTTP 500):
```json
{
  "error": "Internal Server Error",
  "message": "Object reference not set to an instance of an object"
}
```
