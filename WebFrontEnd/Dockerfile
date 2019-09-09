FROM nginx

ADD osm /var/www/public/osm
ADD omm.conf /etc/nginx/omm.conf

EXPOSE 80
CMD [ "nginx", "-c", "/etc/nginx/omm.conf" ]