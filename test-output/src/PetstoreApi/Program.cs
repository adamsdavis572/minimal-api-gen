using FluentValidation;
using PetstoreApi.Extensions;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen(c =>
{
    c.SwaggerDoc("1.0.0", new Microsoft.OpenApi.Models.OpenApiInfo
    {
        Title = "OpenAPI Petstore",
        Description = "This is a sample server Petstore server. For this sample, you can use the api key `special-key` to test the authorization filters.",
        Version = "1.0.0"
    });
});

builder.Services.AddValidatorsFromAssemblyContaining<Program>();

var app = builder.Build();

// Configure the HTTP request pipeline
if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI(c =>
    {
        c.SwaggerEndpoint("/1.0.0/swagger.json", "OpenAPI Petstore 1.0.0");
    });
}

app.UseHttpsRedirection();
app.UseRouting();

app.MapAllEndpoints();

app.Run();

// Make Program accessible for testing
public partial class Program { }