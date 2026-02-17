using FluentValidation;
using PetstoreApi.Extensions;
using PetstoreApi.Contracts.Extensions;
using PetstoreApi.Filters;
using Microsoft.AspNetCore.Authentication.JwtBearer;

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



// Validators
builder.Services.AddApiValidators();

// Handlers
builder.Services.AddApiHandlers();

// Application services
builder.Services.AddApplicationServices();

// === AUTHENTICATION & AUTHORIZATION CONFIGURATION ===
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.Authority = builder.Configuration["Auth:Authority"];
        options.Audience = builder.Configuration["Auth:Audience"];
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