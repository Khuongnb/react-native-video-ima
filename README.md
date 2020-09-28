## A react-native-video with Google IMA SDK

Please feel free to clone this project and add it to your project :D This is not a well maintenance package so I won't put it to npmjs

### Currently only react-native-video v4.4.5 have IMA SDK

**Installation**

```
npm install ./<path-to-your-package>/react-native-video-ima
```
```
react-native link react-native-video-ima
```
**Manually setup** please follow [react-native-video](https://github.com/react-native-community/react-native-video) instruction. Remember to change react-native-video to react-native-video-ima


**Usage**
```
import Video, {AdAction, AdEventType} from 'react-native-video-ima'

class Player extends React.Component {
  ....
  onAdEvent(event) {
    if (event.type === AdEventType.STARTED) {
        this.setState({pause: true})
    }
    if (event.type === AdEventType.COMPLETE) {
        this.setState({pause: false})
    }
    if (event.type === AdEventType.TAPPED) {
        this.player.dispatch(AdAction.RESUME_AD)
    }
  }
  
  onProgress(progress) {
    if (!this.ads) {
      this.ads = true
      const ads = 'https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpostpod&cmsid=496&vid=short_onecue&correlator='
      this.player.dispatch(AdAction.REQUEST_ADS, ads)
    }
  }
  
  render() {
    return (
        <Video
            ...
            ref={ref => this.player = ref}
            onProgress={(progress)=>this.onProgress(progress)}
            paused={this.state.pause}
            onAdEvent={(event)=>this.onAdEvent(event)}
        />
    )
  }
}
```
