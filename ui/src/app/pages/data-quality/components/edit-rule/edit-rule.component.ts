import { Component, Input, OnInit } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SnackbarService } from 'src/app/core/services/snackbar.service';
import { FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';

@Component({
  selector: 'app-edit-rule',
  templateUrl: './edit-rule.component.html',
  styleUrls: ['./edit-rule.component.scss']
})
export class EditRuleComponent {
  @Input() projectId: number;
  @Input() userId: number;
  @Input() ruleData: DataQuailtyListResponse;


  fg: FormGroup;
  public queryRuleTypes: string;
  public queryDcLevel: string;
  public queryDpLevel: string;
  public queryDvLevel: string;
  public isTest: boolean = true;
  public selectedCompleteName: string;
  public completenessData: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ];

  public selectedDataProfiler: string;
  public dataProfiler: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ];

  public selectedDuplicateDataOne: string;
  public duplicateDataOne: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ]

  public duplicateDataTwo: any = [
    "Salary",
		"Basic",
		"Gross",
		"Age"
  ]

  constructor(
    public modal: NgbActiveModal,
    private snakbar: SnackbarService,
    private fb: FormBuilder,
  ){

  }

  ngOnInit(): void {
    
    this.fg = this.fb.group({
      ruleType: new FormControl('', Validators.required),
      dcLevel: new FormControl('', Validators.required),
      dpLevel: new FormControl('', Validators.required),
      dvLevel: new FormControl('', Validators.required),
      relatedAttributes: new FormControl('', Validators.required),
      relatedDataProfiler: new FormControl('', Validators.required),
      relatedDuplicateDataOne: new FormControl('',),
      relatedDuplicateDataTwo: new FormControl('',),
      dcRowPercentage: new FormControl('',),
      dcColumnPercentage: new FormControl('',),
      dpColumnPercentage: new FormControl('',),
      dpTablePercentage: new FormControl('',),
      nullPercentage: new FormControl('',),
      dvDuplicatePercentage: new FormControl('',),
      dvColumnPercentage: new FormControl('',),
      dvMultColumnPercentage: new FormControl('',),
      // dvLevel: new FormControl('', Validators.required),
    });
  }

  public onItemChange(value: string): void {
    console.log(this.queryRuleTypes);
    
  }
}
