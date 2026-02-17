using FluentValidation;
using PetstoreApi.Extensions;
using PetstoreApi.Contracts.Extensions;
using PetstoreApi.Filters;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.IdentityModel.Tokens.Jwt;
using Microsoft.AspNetCore.Mvc;

// Clear default claim type mappings to preserve original JWT claim names
JwtSecurityTokenHandler.DefaultInboundClaimTypeMap.Clear();

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new PetstoreApi.Converters.EnumMemberJsonConverterFactory());
    options.SerializerOptions.PropertyNameCaseInsensitive = true;
});

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("v1", new Microsoft.OpenApi.Models.OpenApiInfo
    {
        Title = "Petstore API",
        Description = "Petstore sample API",
        Version = "v1"
    });
});
builder.Services.AddProblemDetails();
// Register validators from Contracts package
builder.Services.AddApiValidators();
builder.Services.AddTransient(typeof(MediatR.IPipelineBehavior<,>), typeof(PetstoreApi.Behaviors.ValidationBehavior<,>));
// Register handlers from Implementation assembly
builder.Services.AddApiHandlers();
builder.Services.AddApplicationServices();


// === AUTHENTICATION & AUTHORIZATION CONFIGURATION ===
// Well-known test secret for JWT signing (must match bruno/generate-test-tokens.js)
const string TestSecret = "this-is-a-test-secret-key-for-petstore-api-dev-only-min-32-bytes!";

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.MapInboundClaims = false; // Preserve original JWT claim names (e.g. "sub" not "http://schemas...")
        
        // For testing: Validate tokens signed with known test secret
        if (builder.Environment.IsDevelopment())
        {
            options.RequireHttpsMetadata = false;
            options.TokenValidationParameters = new TokenValidationParameters
            {
                ValidateIssuer = false,
                ValidateAudience = false,
                ValidateLifetime = false,
                ValidateIssuerSigningKey = true,
                IssuerSigningKey = new SymmetricSecurityKey(
                    System.Text.Encoding.UTF8.GetBytes(TestSecret))
            };
        }
        else
        {
            options.Authority = builder.Configuration["Auth:Authority"];
            options.Audience = builder.Configuration["Auth:Audience"];
        }
    });

builder.Services.AddAuthorization(options =>
{
    // Authorization policies for endpoint filter
    options.AddPolicy("ReadAccess", policy => 
        policy.RequireClaim("permission", "read"));
    options.AddPolicy("WriteAccess", policy => 
        policy.RequireClaim("permission", "write"));
});

// Register the permission filter
builder.Services.AddSingleton<PermissionEndpointFilter>();

var app = builder.Build();

// Configure the HTTP request pipeline
// Global exception handler for validation and model binding errors
app.UseExceptionHandler(exceptionHandlerApp =>
{
    exceptionHandlerApp.Run(async context =>
    {
        var exceptionHandlerPathFeature = context.Features.Get<Microsoft.AspNetCore.Diagnostics.IExceptionHandlerPathFeature>();
        var exception = exceptionHandlerPathFeature?.Error;

        if (exception is FluentValidation.ValidationException validationException)
        {
            context.Response.StatusCode = StatusCodes.Status400BadRequest;
            context.Response.ContentType = "application/problem+json";
            
            var problemDetails = new Microsoft.AspNetCore.Http.HttpValidationProblemDetails(
                validationException.Errors
                    .GroupBy(x => x.PropertyName)
                    .ToDictionary(
                        g => g.Key,
                        g => g.Select(x => x.ErrorMessage).ToArray()
                    ))
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "Validation failed",
                Type = "https://tools.ietf.org/html/rfc7231#section-6.5.1"
            };

            await context.Response.WriteAsJsonAsync(problemDetails);
        }
        else if (exception is Microsoft.AspNetCore.Http.BadHttpRequestException badRequestException)
        {
            context.Response.StatusCode = StatusCodes.Status400BadRequest;
            context.Response.ContentType = "application/problem+json";
            
            var problemDetails = new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "Bad Request",
                Detail = badRequestException.Message,
                Type = "https://tools.ietf.org/html/rfc7231#section-6.5.1"
            };

            await context.Response.WriteAsJsonAsync(problemDetails);
        }
        else if (exception is System.Text.Json.JsonException jsonException)
        {
            context.Response.StatusCode = StatusCodes.Status400BadRequest;
            context.Response.ContentType = "application/problem+json";
            
            var problemDetails = new ProblemDetails
            {
                Status = StatusCodes.Status400BadRequest,
                Title = "Invalid JSON",
                Detail = "The request body contains invalid JSON",
                Type = "https://tools.ietf.org/html/rfc7231#section-6.5.1"
            };

            await context.Response.WriteAsJsonAsync(problemDetails);
        }
        else
        {
            context.Response.StatusCode = StatusCodes.Status500InternalServerError;
            context.Response.ContentType = "application/problem+json";
            
            var problemDetails = new ProblemDetails
            {
                Status = StatusCodes.Status500InternalServerError,
                Title = "An error occurred",
                Detail = app.Environment.IsDevelopment() ? exception?.Message : "An unexpected error occurred",
                Type = "https://tools.ietf.org/html/rfc7231#section-6.6.1"
            };

            await context.Response.WriteAsJsonAsync(problemDetails);
        }
    });
});

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

// === AUTHENTICATION & AUTHORIZATION MIDDLEWARE ===
app.UseAuthentication();
app.UseAuthorization();

// === ENDPOINT REGISTRATION ===
// OPTION 1: Without authorization
// app.AddApiEndpoints();

// OPTION 2: With authorization (ENABLED)
app.AddAuthorizedApiEndpoints();

// Health check endpoint
app.MapGet("/health", () => Results.Ok(new { status = "healthy" }))
    .WithName("HealthCheck")
    .WithTags("Health")
    .Produces(200);


app.Run();

// Make Program class accessible to tests
public partial class Program { }