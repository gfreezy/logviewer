CREATE DATABASE logviewer;

CREATE USER 'logviewer'@"%" IDENTIFIED BY 'logviewer';
GRANT ALL ON *.* TO logviewer@"%";
