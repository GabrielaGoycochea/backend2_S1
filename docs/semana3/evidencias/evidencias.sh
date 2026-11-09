#!/bin/bash
# Ejecuta las pruebas de amenazas contra la API levantada y deja la evidencia en texto.
B=http://localhost:8080
CL='Petsalud2026!'
J='Content-Type: application/json'
tok() { curl -s -X POST $B/api/auth/login -H "$J" -d "{\"email\":\"$1\",\"password\":\"$2\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])'; }
caso() { # titulo, esperado, curl args...
  local titulo="$1" esperado="$2"; shift 2
  local cuerpo; cuerpo=$(mktemp); local cab; cab=$(mktemp)
  local cod; cod=$(curl -s -o "$cuerpo" -D "$cab" -w '%{http_code}' "$@")
  local ok="OK"; [ "$cod" != "$esperado" ] && ok="REVISAR"
  printf '%-58s esperado=%s obtenido=%s %s\n' "$titulo" "$esperado" "$cod" "$ok"
  if [ -s "$cuerpo" ]; then printf '    cuerpo: %s\n' "$(head -c 160 "$cuerpo" | tr -d '\n')"; fi
  rm -f "$cuerpo" "$cab"
}
TC=$(tok cliente@petsalud.cl "$CL"); TV=$(tok vet@petsalud.cl "$CL"); TA=$(tok admin@petsalud.cl "$CL")
ID=$(curl -s $B/api/usuarios -H "Authorization: Bearer $TA" | python3 -c '
import sys,json; u={x["email"]:x["id"] for x in json.load(sys.stdin)}; print(u["cliente@petsalud.cl"], u["vet@petsalud.cl"], u["admin@petsalud.cl"])')
read IDC IDV IDA <<< "$ID"
echo "== Usuarios del perfil prueba: cliente=$IDC vet=$IDV admin=$IDA"
LUNA=$(curl -s -X POST $B/api/mascotas/propietario/$IDC -H "Authorization: Bearer $TC" -H "$J" -d '{"nombre":"Luna","especie":"Perro","raza":"Mestiza","sexo":"Hembra","fechaNacimiento":"2021-05-10"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
echo "== Mascota Luna creada por el cliente: id=$LUNA"
echo
echo "== 1. SQL Injection"
caso "Login con ' OR '1'='1' --" 401 -X POST $B/api/auth/login -H "$J" -d "{\"email\":\"' OR '1'='1' --\",\"password\":\"' OR '1'='1' --\"}"
caso "Registro con inyeccion en el email" 400 -X POST $B/api/usuarios/registro -H "$J" -d "{\"email\":\"cliente@petsalud.cl' OR '1'='1\",\"password\":\"Clave1234\",\"nombre\":\"Ana\"}"
caso "Busqueda por categoria con inyeccion" 400 -G $B/api/productos/categoria --data-urlencode "categoria=Alimentos' OR '1'='1' --"
caso "Id de ruta /api/mascotas/1 OR 1=1" 400 "$B/api/mascotas/1%20OR%201=1" -H "Authorization: Bearer $TC"
echo
echo "== 2. XSS"
caso "Script en el nombre de la mascota (CLIENTE)" 400 -X POST $B/api/mascotas/propietario/$IDC -H "Authorization: Bearer $TC" -H "$J" -d "{\"nombre\":\"<script>alert('xss')</script>\",\"especie\":\"Perro\"}"
caso "HTML en el diagnostico (VETERINARIO)" 400 -X POST "$B/api/expedientes?mascotaId=$LUNA&veterinarioId=$IDV" -H "Authorization: Bearer $TV" -H "$J" -d '{"descripcion":"Control anual","diagnostico":"<img src=x onerror=alert(1)>"}'
caso "Script en el motivo de la cita (CLIENTE)" 400 -X POST -G $B/api/citas -H "Authorization: Bearer $TC" --data-urlencode "mascotaId=$LUNA" --data-urlencode "veterinarioId=$IDV" --data-urlencode "fechaHora=2026-12-01T10:00:00" --data-urlencode "motivo=<script>alert(1)</script>"
caso "Datos validos: expediente de Luna (VETERINARIO)" 200 -X POST "$B/api/expedientes?mascotaId=$LUNA&veterinarioId=$IDV" -H "Authorization: Bearer $TV" -H "$J" -d '{"descripcion":"Control anual","diagnostico":"Paciente sano","tratamiento":"Vacuna antirrabica anual"}'
echo "    cabeceras de GET /api/productos:"
curl -s -D - -o /dev/null $B/api/productos | grep -i -E "content-security|x-content-type|x-frame|referrer|cache-control" | sed 's/^/      /'
echo
echo "== 3. Asignacion masiva (otro cliente intenta sobrescribir a Luna enviando su id)"
curl -s -o /dev/null -X POST $B/api/usuarios/registro -H "$J" -d '{"email":"otro.cliente@petsalud.cl","password":"Clave1234","nombre":"Otro","apellido":"Cliente"}'
TO=$(tok otro.cliente@petsalud.cl Clave1234)
IDO=$(curl -s $B/api/usuarios -H "Authorization: Bearer $TA" | python3 -c 'import sys,json;print([x["id"] for x in json.load(sys.stdin) if x["email"]=="otro.cliente@petsalud.cl"][0])')
caso "POST con \"id\":$LUNA en el cuerpo" 200 -X POST $B/api/mascotas/propietario/$IDO -H "Authorization: Bearer $TO" -H "$J" -d "{\"id\":$LUNA,\"nombre\":\"Robada\",\"especie\":\"Gato\"}"
echo "    Luna sigue intacta: $(curl -s $B/api/mascotas/$LUNA -H "Authorization: Bearer $TC" | python3 -c 'import sys,json;m=json.load(sys.stdin);print(m["nombre"], "propietario", m["propietario"]["email"])')"
caso "El otro cliente consulta a Luna (ajena)" 403 $B/api/mascotas/$LUNA -H "Authorization: Bearer $TO"
echo
echo "== 4. CSRF"
caso "POST con cookie de sesion y sin Bearer" 401 -X POST $B/api/productos -b "JSESSIONID=sesion-robada" -H "$J" -d '{"nombre":"Alimento perro 15 kg","precio":24990,"stock":20,"categoria":"Alimentos"}'
echo "    Set-Cookie en el login: $(curl -s -D - -o /dev/null -X POST $B/api/auth/login -H "$J" -d "{\"email\":\"cliente@petsalud.cl\",\"password\":\"$CL\"}" | grep -ci set-cookie) (0 = no crea sesion)"
caso "Preflight desde https://sitio-malicioso.example" 403 -X OPTIONS $B/api/productos -H "Origin: https://sitio-malicioso.example" -H "Access-Control-Request-Method: POST"
caso "Preflight desde http://localhost:3000 (front autorizado)" 200 -X OPTIONS $B/api/productos -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: POST"
echo
echo "== 5. Roles con @PreAuthorize"
caso "CLIENTE envia notificacion" 403 -X POST $B/api/notificaciones/usuario/$IDC -H "Authorization: Bearer $TC" -H "$J" -d '{"mensaje":"Campana de vacunacion","tipo":"Promocion"}'
caso "ADMIN envia notificacion" 200 -X POST $B/api/notificaciones/usuario/$IDC -H "Authorization: Bearer $TA" -H "$J" -d '{"mensaje":"Campana de vacunacion","tipo":"Promocion"}'
EXP=$(curl -s "$B/api/expedientes/mascota/$LUNA" -H "Authorization: Bearer $TV" | python3 -c 'import sys,json;print(json.load(sys.stdin)[0]["id"])')
caso "VETERINARIO elimina expediente $EXP" 403 -X DELETE $B/api/expedientes/$EXP -H "Authorization: Bearer $TV"
caso "CLIENTE consulta expediente $EXP" 403 $B/api/expedientes/$EXP -H "Authorization: Bearer $TC"
caso "ADMIN elimina expediente $EXP" 204 -X DELETE $B/api/expedientes/$EXP -H "Authorization: Bearer $TA"
echo
echo "== 6. JWT y monitoreo"
MALO="${TV%?}x"; [ "$MALO" = "$TV" ] && MALO="${TV%?}y"
caso "JWT con la firma alterada" 401 "$B/api/productos/stock-bajo?cantidad=5" -H "Authorization: Bearer $MALO"
caso "Sin token a un recurso protegido" 401 $B/api/usuarios
for i in 1 2 3 4 5; do curl -s -o /dev/null -X POST $B/api/auth/login -H "$J" -d '{"email":"otro.cliente@petsalud.cl","password":"Incorrecta'$i'"}'; done
caso "Sexto intento tras 5 fallos (clave correcta)" 429 -X POST $B/api/auth/login -H "$J" -d '{"email":"otro.cliente@petsalud.cl","password":"Clave1234"}'
for i in $(seq 1 200); do curl -s -o /dev/null $B/api/productos; done
echo "    200 solicitudes seguidas a /api/productos enviadas (umbral de rafaga: 200 en 15 min)"
