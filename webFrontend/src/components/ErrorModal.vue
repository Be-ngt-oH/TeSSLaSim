<template>
  <div class="ui basic modal">
    <div class="ui icon header">
      <i class="bug icon"></i>
      <p class="content title">
        Title
      </p>
    </div>
    <div class="content">
      <p class="message">Message</p>
      <div class="ui divider"></div>
      <p>If you're a dev or simply interested, check the browser console for additional information.</p>
    </div>
    <div class="actions">
      <div class="ui green ok basic inverted button">
        <i class="checkmark icon"></i>
        Ok
      </div>
    </div>
  </div>
</template>

<script>
  import $ from 'jquery'

  /* ui modal has dependencies on both dimmer and transiion */
  import 'semantic-ui-css/components/modal'
  import 'semantic-ui-css/components/dimmer'
  import 'semantic-ui-css/components/transition'

  import { Errors } from '../error'

  export default {
    name: 'error-modal',
    methods: {
      display (error) {
        let $modal = $(this.$el)
        $modal.find('.title').text(error.title)
        $modal.find('.message').text(error.message)

        if (error === Errors.NETWORK_ERROR) {
          $modal.find('.header .icon').removeClass('bug').addClass('frown')
        } else {
          $modal.find('.header .icon').removeClass('frown').addClass('bug')
        }

        $modal.modal('show')
      }
    }
  }
</script>
