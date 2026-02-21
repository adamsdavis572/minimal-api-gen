## Backlog

*  apiVersion in route path and package version from OAS version (info.version)
*  clean up documaenation include package structure which is incorrect
   *  simplify configuration of generator - do we need ProblemDetails as an option? etc 
*  update the generator to generate a default implmentation of Handler mapping to Model and calling a service inteface to handle it. these will be likely be overidded but can still act as a template
*  versions in endpoint - this still needs to eb implemented
* AddAuthorizedApiEndpoints should just be AddApiEndpoints?? but adding .AddEndpointFilter<PermissionEndpointFilter>();
* Model Validators - are these still need now we have DTO validators 
* ExceptionHandlingExtensions.cs moved to Contracts? 
* SecurityExtensions.cs moved to impl directory and generated - is this a good idea? 
* Load via assembly scans? 
* 