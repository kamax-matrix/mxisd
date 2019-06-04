# Web pages for the 3PID sessions
You can customize the various pages used during a 3PID validation using the options below.

## Configuration
Pseudo-configuration to illustrate the structure:
```yaml
# CONFIGURATION EXAMPLE
# DO NOT COPY/PASTE THIS IN YOUR CONFIGURATION
view:
  session:
    onTokenSubmit:
      success: '/path/to/session/tokenSubmitSuccess-page.html'
      failure: '/path/to/session/tokenSubmitFailure-page.html'
# CONFIGURATION EXAMPLE
# DO NOT COPY/PASTE THIS IN YOUR CONFIGURATION
```

`view.session`:
This is triggered when a user submit a validation token for a 3PID session. It is typically visited when clicking the
link in a validation email.

The template should typically inform the user that the validation was successful and to go back in their Matrix client
to finish the validation process, or that the validation failed.

Two configuration keys are available that accept paths to HTML templates:
- `success`
- `failure`

### Serving static assets
mxisd will not serve any static asset (images, JS, CSS, etc.). If such are needed, you will need to serve them using the
reverse proxy sitting in front of mxisd using a path outside of the `/_matrix/identity/` namespace. We advise using
the base path `/static/` for such use cases, allowing to remain under the same hostname/origin.

You can also serve such assets using absolute URL, possibly under other domains, but be aware of Cross-Origin restrictions
in browsers which are out of scope of mxisd.

## Placeholders
### Success
No object/placeholder are currently available.

### Failure
No object/placeholder are currently available.
