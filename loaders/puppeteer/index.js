const puppeteer = require('puppeteer')
const process = require('process')
const url = require('url')
const uuidv4 = require('uuid/v4')
const JSON5 = require('json5')

// https://github.com/puppeteer/puppeteer/blob/v10.0.0/docs/api.md
// https://github.com/checkly/puppeteer-examples
// https://github.com/checkly/puppeteer-examples/blob/master/1.%20basics/location_faker.js
// https://github.com/checkly/puppeteer-examples/blob/master/1.%20basics/set_cookie.js

if (!process.env.hasOwnProperty('LOAD_CONFIG')) {
  console.log('No load config for puppeeteer provided, exiting ...')
  process.exit(1)
}

const config = JSON5.parse(process.env.LOAD_CONFIG)
const apm = JSON5.parse(process.env.APM_CONFIG)

if (typeof config.wait !== 'number') {
  config.wait = 0
}

if ( config.browser == undefined ) {
  config.browser = "chrome"
}

let appKey = undefined
let beaconUrlHttp = undefined
if (typeof apm.eum === 'object') {
  appKey = apm.eum['appKey']
  beaconUrlHttp = apm.eum['beaconUrlHttp']
}
console.log('==> eum.appkey [' + appKey + ']')
console.log('==> eum.beaconUrlHttp [' + beaconUrlHttp + ']')

function run() {
  (async() => {

    while (true) {
      console.log('========================================')
      console.log('  => browser [' + config.browser + ']')

      // https://github.com/puppeteer/puppeteer/blob/main/src/common/DeviceDescriptors.ts
      let device = undefined
      if ( config.devices != undefined ) {
        device = puppeteer.devices[config.devices[Math.floor(Math.random() * config.devices.length)]];
        console.log('  => device [' + device['name'] + ']')
      }

      const browser = await puppeteer.launch({
        headless: true,
        product: config.browser,
        args: ["--no-sandbox", "--disable-setuid-sandbox"],
        ignoreHTTPSErrors: true
      });

      const page = await browser.newPage()
      if ( device != undefined ) {
        await page.emulate(device)
      }

      // https://github.com/checkly/puppeteer-examples/blob/master/1.%20basics/location_faker.js
      let location = undefined
      if ( config.locations != undefined ) {
        location = config.locations[Math.floor(Math.random() * config.locations.length)];
        console.log('  => location [' + JSON5.stringify(location) + ']')
      }

      page.on('requestfinished', function (request) {
        console.log('  -- ' + request.url())
      })

      var uniqueId = uuidv4()
      for (var i = 0; i < config.urls.length; i++) {
        const myUrl = new url.URL(config.urls[i])
        var searchParams = myUrl.searchParams
        searchParams.append('unique_session_id', uniqueId)
        myUrl.search = searchParams
        console.log('  => Visiting', myUrl.href)
        try {
          if ( location != undefined ) {
            searchParams.append('city', location['city'])
            searchParams.append('region', location['region'])
            searchParams.append('country', location['country'])
            myUrl.search = searchParams
          }
  
          await page.goto(myUrl, { waitUntil: 'networkidle0' })
          if ( appKey != undefined ) {
            console.log('  => Waiting ADRUM Response')
            const adrumResponse = await page.waitForResponse(beaconUrlHttp + '/eumcollector/beacons/browser/v1/'+ appKey + '/adrum');
            console.log('  => ADRUM Response [' + adrumResponse.status() + ']')
          }

        } catch (e) {
          console.log('>>>>>>> ERROR <<<<<<<')
          console.log(e)
          await new Promise(resolve => setTimeout(resolve, 1500))
        }
      }

      await browser.close()
    }
    
  })()
}

setTimeout(run, config.wait * 1000)
