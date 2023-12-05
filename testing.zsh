username="xx_test_${RANDOM}_xx"
bus_stop_id="3368"
bus_id="C1"
headers=(-H "Content-Type: application/json" -H "Origin: http://localhost:3000")
subscription='{"endpoint":"https://updates.push.services.mozilla.com/wpush/v2/gAAAAABlbr3xA0Ka9WbZB-jhJM7R-FZOyFeErMeqMNP9yeirsEcDftLK8GwzEqAwuR70OYoAKm2P1rvXquo_NcSuSEHX6IaPZsr4OB4iRF2C2gZMb37R9BKKOSFjjbbLD_H6QO__zLyusI4bCm4vDdrQ4SY2a0rlnZHyLkq0gpSBlV8ag5fdNe8","expirationTime":null,"keys":{"auth":"znVGgyruX84H1rRhJckQNg==","p256dh":"BJQSn+u8vp8jovhMhMN39z815QZvlQKd0h4XXNVyXSft1HCiKL+84RU9A28HI5nXLcjdB5WD8Bcr7QE9Zivi4Y0="}}'

echo "--- LOGGING IN ---"
token=$( { curl -X POST localhost:8080/register $headers -d @- <<EOF  || exit 1 } | jq -r .token
{
    "username"  : "${username}",
    "password"  : "test",
    "firstname" : "test",
    "lastname"  : "test"
}'
EOF
)
echo "Got token: $token"
headers=($headers -H "Authorization: Bearer $token")

echo "--- MAKING BROWSER ENDPOINT ---"
curl -X POST localhost:8080/browserEndpoints $headers -i -d "${subscription}" || exit 1

echo "--- MAKING SUBSCRIPTION ---"
curl -X POST localhost:8080/dublinBusSubscriptions $headers -i -d @-  <<EOF || exit 1
{
    "endpoint"  : $(jq .endpoint <<< $subscription),
    "busStopId" : "${bus_stop_id}",
    "busId"     : "${bus_id}"
}
EOF

echo "--- MANIPULATING ACTIVE TIME RANGES ---"
date                     +'%A'    | read day
date --date="1   minute ago" +'%H %M' | read start_hour start_minute
date --date="31 minutes" +'%H %M' | read end_hour   end_minute
curl -X POST localhost:8080/dublinBusSubscriptions/${bus_stop_id}/${bus_id}/activeTimeRanges $headers -i -d @- <<EOF || exit 1
{
    "day"         : "${day:u}",
    "startHour"   : "${start_hour}",
    "startMinute" : "${start_minute}",
    "endHour"     : "${end_hour}",
    "endMinute"   : "${end_minute}"
}
EOF

echo "--- FETCHING THE DATA ---"
curl localhost:8080/user $headers -i || exit 1

echo "--- FETCHING THE TRIPS ---"
curl localhost:8080/generalBusStopUpdates $headers -i || exit 1

echo "--- FETCHING THE ENDPOINTS ---"
curl localhost:8080/browserEndpoints $headers -i || exit 1
