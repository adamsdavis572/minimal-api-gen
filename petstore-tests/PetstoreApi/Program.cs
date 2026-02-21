using PetstoreApi.Extensions;
using PetstoreApi.Contracts.Extensions;

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

// Register application services (IPetStore, etc.)
builder.Services.AddApplicationServices();

// Configure authentication & authorization
builder.Services.AddApiSecurity(builder.Configuration, builder.Environment);

var app = builder.Build();

// Configure the HTTP request pipeline
app.UseApiExceptionHandler(builder.Environment);

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();

// Register API endpoints with authorization
app.AddAuthorizedApiEndpoints();

// Health check endpoint
app.MapGet("/health", () => Results.Ok(new { status = "healthy" }))
    .WithName("HealthCheck")
    .WithTags("Health")
    .Produces(200);

app.Run();

// Make Program class accessible to tests
public partial class Program { }