import { defineComponent, h } from 'vue'

export const ElFormStub = defineComponent({
  name: 'ElFormStub',
  setup(_, { slots, expose }) {
    expose({
      validate: async () => true
    })
    return () => h('form', {}, slots.default?.())
  }
})

export const ElFormItemStub = defineComponent({
  name: 'ElFormItemStub',
  props: {
    label: String
  },
  setup(props, { slots }) {
    return () => h('label', {}, [
      props.label ? h('span', props.label) : null,
      slots.default?.()
    ])
  }
})

export const ElInputStub = defineComponent({
  name: 'ElInputStub',
  inheritAttrs: false,
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue', 'keyup.enter'],
  setup(props, { attrs, emit }) {
    const { size, ...restAttrs } = attrs
    return () =>
      h('input', {
        ...restAttrs,
        value: props.modelValue,
        onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement).value),
        onKeyup: (event: KeyboardEvent) => {
          if (event.key === 'Enter') {
            emit('keyup.enter')
          }
        }
      })
  }
})

export const ElSelectStub = defineComponent({
  name: 'ElSelectStub',
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  setup(props, { slots, emit }) {
    return () =>
      h(
        'select',
        {
          value: props.modelValue,
          onChange: (event: Event) => emit('update:modelValue', (event.target as HTMLSelectElement).value)
        },
        slots.default?.()
      )
  }
})

export const ElOptionStub = defineComponent({
  name: 'ElOptionStub',
  props: {
    label: String,
    value: {
      type: [String, Number],
      default: ''
    }
  },
  setup(props) {
    return () => h('option', { value: props.value }, props.label)
  }
})

export const ElButtonStub = defineComponent({
  name: 'ElButtonStub',
  emits: ['click'],
  setup(_, { slots, emit }) {
    return () => h('button', { onClick: () => emit('click') }, slots.default?.())
  }
})

export const ElDialogStub = defineComponent({
  name: 'ElDialogStub',
  props: {
    modelValue: Boolean
  },
  setup(props, { slots }) {
    return () => (props.modelValue ? h('section', {}, [slots.default?.(), slots.footer?.()]) : null)
  }
})

export const ElDrawerStub = defineComponent({
  name: 'ElDrawerStub',
  props: {
    modelValue: Boolean
  },
  setup(props, { slots }) {
    return () =>
      props.modelValue
        ? h('aside', { class: 'el-drawer-stub' }, [slots.default?.(), slots.footer?.()])
        : null
  }
})

export const ElTagStub = defineComponent({
  name: 'ElTagStub',
  setup(_, { slots }) {
    return () => h('span', { class: 'el-tag-stub' }, slots.default?.())
  }
})

export const ElPaginationStub = defineComponent({
  name: 'ElPaginationStub',
  setup() {
    return () => h('div', { class: 'el-pagination-stub' })
  }
})

export const ElSkeletonStub = defineComponent({
  name: 'ElSkeletonStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'el-skeleton-stub' }, slots.default?.())
  }
})

export const ElTableStub = defineComponent({
  name: 'ElTableStub',
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  setup(props) {
    return () => h('pre', { class: 'el-table-stub' }, JSON.stringify(props.data))
  }
})

export const ElTableColumnStub = defineComponent({
  name: 'ElTableColumnStub',
  setup() {
    return () => null
  }
})

export const ElDropdownStub = defineComponent({
  name: 'ElDropdownStub',
  emits: ['command'],
  setup(_, { slots }) {
    return () => h('div', { class: 'el-dropdown-stub' }, [slots.default?.(), slots.dropdown?.()])
  }
})

export const ElDropdownMenuStub = defineComponent({
  name: 'ElDropdownMenuStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'el-dropdown-menu-stub' }, slots.default?.())
  }
})

export const ElDropdownItemStub = defineComponent({
  name: 'ElDropdownItemStub',
  emits: ['click'],
  setup(_, { slots, emit }) {
    return () => h('button', { type: 'button', onClick: () => emit('click') }, slots.default?.())
  }
})

export const ElRadioGroupStub = defineComponent({
  name: 'ElRadioGroupStub',
  props: {
    modelValue: {
      type: [String, Number],
      default: ''
    }
  },
  emits: ['update:modelValue'],
  setup(_, { slots }) {
    return () => h('div', { class: 'el-radio-group-stub' }, slots.default?.())
  }
})

export const ElRadioButtonStub = defineComponent({
  name: 'ElRadioButtonStub',
  props: {
    label: {
      type: [String, Number],
      default: ''
    }
  },
  setup(props, { slots }) {
    return () => h('button', { type: 'button', 'data-label': props.label }, slots.default?.())
  }
})

export const ElTabsStub = defineComponent({
  name: 'ElTabsStub',
  props: {
    modelValue: {
      type: String,
      default: ''
    }
  },
  emits: ['update:modelValue'],
  setup(_, { slots }) {
    return () => h('div', { class: 'el-tabs-stub' }, slots.default?.())
  }
})

export const ElTabPaneStub = defineComponent({
  name: 'ElTabPaneStub',
  props: {
    label: String,
    name: String
  },
  setup(props, { slots }) {
    return () =>
      h('section', { class: 'el-tab-pane-stub', 'data-name': props.name }, [
        props.label ? h('h4', props.label) : null,
        slots.default?.()
      ])
  }
})

export const ElCheckboxGroupStub = defineComponent({
  name: 'ElCheckboxGroupStub',
  props: {
    modelValue: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  setup(_, { slots }) {
    return () => h('div', { class: 'el-checkbox-group-stub' }, slots.default?.())
  }
})

export const ElCheckboxStub = defineComponent({
  name: 'ElCheckboxStub',
  props: {
    value: {
      type: [String, Number, Boolean],
      default: ''
    }
  },
  setup(props, { slots }) {
    return () => h('label', { class: 'el-checkbox-stub', 'data-value': props.value }, slots.default?.())
  }
})

export const ElEmptyStub = defineComponent({
  name: 'ElEmptyStub',
  props: {
    description: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    return () => h('div', { class: 'el-empty-stub' }, props.description)
  }
})

export const elementPlusStubs = {
  ElForm: ElFormStub,
  ElFormItem: ElFormItemStub,
  ElInput: ElInputStub,
  ElSelect: ElSelectStub,
  ElOption: ElOptionStub,
  ElButton: ElButtonStub,
  ElDialog: ElDialogStub,
  ElDrawer: ElDrawerStub,
  ElTag: ElTagStub,
  ElPagination: ElPaginationStub,
  ElSkeleton: ElSkeletonStub,
  ElTable: ElTableStub,
  ElTableColumn: ElTableColumnStub,
  ElDropdown: ElDropdownStub,
  ElDropdownMenu: ElDropdownMenuStub,
  ElDropdownItem: ElDropdownItemStub,
  ElRadioGroup: ElRadioGroupStub,
  ElRadioButton: ElRadioButtonStub,
  ElTabs: ElTabsStub,
  ElTabPane: ElTabPaneStub,
  ElCheckboxGroup: ElCheckboxGroupStub,
  ElCheckbox: ElCheckboxStub,
  ElEmpty: ElEmptyStub
}
