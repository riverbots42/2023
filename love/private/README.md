= Internal Server Code

== Introduction

This is the code for http://lab.riverbots.org, which is an internal server that runs the label printer
and has the administrative UI for the admin UI for https://love.riverbots.org

== Subdirectories

The three subs are:

   1. [admin/](admin/) - The files that define the website http://lab.riverbots.org (the admin UI itself)
   2. [base/](base/) - The files that set up the server itself, such as the usernames/passwords, ddclient (points lab.riverbots.org to the right IP), the VPN to simulator.riverbots.org (so we can talk to the database).
   3. [zebra/](zebra/) - The files that define the website http://lab.riverbots.org:8080, which is the HTTP interface to the label printer (takes a PNG and converts it to the printer's page definition language).
